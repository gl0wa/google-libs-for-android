/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.feeds.widget;

import com.google.android.feeds.provider.FeedContract;
import com.google.android.feeds.provider.FeedUri;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.Observable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * A {@link CursorAdapter} that manages asynchronous queries.
 */
public abstract class BaseFeedAdapter extends CursorAdapter implements FeedAdapter {

    /**
     * Listens for changes to the query.
     *
     * @see BaseFeedAdapter#changeQuery(Uri, String[], String, String[],
     *      String)
     * @see BaseFeedAdapter#replaceQuery(Uri, String[], String, String[],
     *      String)
     * @see BaseFeedAdapter#clear()
     */
    public interface OnQueryChangeListener {
        /**
         * Notifies the listener that the query has changed or has been cleared.
         *
         * @param adapter the adapter that changed.
         */
        void onQueryChange(BaseFeedAdapter adapter);
    }

    private static class QueryObservable extends Observable<OnQueryChangeListener> {
        public void notifyChanged(BaseFeedAdapter adapter) {
            synchronized (mObservers) {
                for (OnQueryChangeListener observer : mObservers) {
                    observer.onQueryChange(adapter);
                }
            }
        }
    }

    private static class QueryStateObservable extends Observable<OnQueryStateChangeListener> {
        public void notifyChanged(FeedAdapter adapter) {
            synchronized (mObservers) {
                for (OnQueryStateChangeListener observer : mObservers) {
                    observer.onQueryStateChange(adapter);
                }
            }
        }
    }

    private static final String STATE_QUERIES = "feeds:queries";

    private static int getResponseCode(Bundle extras) {
        int defaultValue = HttpURLConnection.HTTP_OK;
        return extras.getInt(FeedContract.EXTRA_RESPONSE_CODE, defaultValue);
    }

    private static int getResponseCode(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return getResponseCode(extras);
    }

    private static String getResponseMessage(Bundle extras) {
        return extras.getString(FeedContract.EXTRA_RESPONSE_MESSAGE);
    }

    private static String getResponseMessage(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return getResponseMessage(extras);
    }

    private static boolean hasError(Bundle extras) {
        int responseCode = getResponseCode(extras);
        return responseCode != HttpURLConnection.HTTP_OK;
    }

    private static boolean hasError(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return hasError(extras);
    }

    private static boolean hasMore(Bundle extras) {
        return extras.getBoolean(FeedContract.EXTRA_MORE);
    }

    private static boolean hasMore(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return hasMore(extras);
    }

    private static Intent getSolution(Bundle extras) {
        return extras.getParcelable(FeedContract.EXTRA_SOLUTION);
    }

    private static Intent getSolution(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return getSolution(extras);
    }

    private static SparseArray<Parcelable> getOrCreateSparseParcelableArray(Bundle bundle,
            String key) {
        SparseArray<Parcelable> array = bundle.getSparseParcelableArray(key);
        if (array == null) {
            array = new SparseArray<Parcelable>();
            bundle.putSparseParcelableArray(key, array);
        }
        return array;
    }

    private static NonBlockingCursor createNonBlockingCursor(Cursor cursor) {
        if (cursor instanceof CrossProcessCursor) {
            CrossProcessCursor crossProcessCursor = (CrossProcessCursor) cursor;
            return new NonBlockingCrossProcessCursor(crossProcessCursor);
        } else if (cursor != null) {
            return new NonBlockingCursor(cursor);
        } else {
            return null;
        }
    }

    private final Context mContext;

    private final int mQueryId;

    /**
     * The current query or {@code null}.
     */
    private ContentQuery mQuery;

    /**
     * A {@link Future} for the active background task, or {@code null} if no
     * background task is currently executing.
     */
    private Future<Cursor> mTask;

    /**
     * Tracks whether or not the {@link Activity} is visible.
     */
    private boolean mVisible;

    /**
     * Mirrors the hidden field {@link CursorAdapter#mDataValid}.
     */
    private boolean mDataValid;

    private Filter mFilter;

    private final QueryObservable mQueryObservable;

    private final QueryStateObservable mQueryStateObservable;

    /**
     * The value of {@link #mQuery} at the beginning of the current transaction.
     */
    private ContentQuery mInitialQuery;

    /**
     * The value of {@link #isQueryPending()} at the beginning of the current
     * transaction.
     */
    private boolean mInitialQueryState;

    private int mTransaction;

    protected BaseFeedAdapter(Context context, int id) {
        super(context, null, false);
        mContext = context;
        mQueryId = id;
        mQueryObservable = new QueryObservable();
        mQueryStateObservable = new QueryStateObservable();
    }

    /**
     * Denotes the beginning of an operation that may change {@link #mQuery} or
     * {@link #isQueryPending()}.
     * <p>
     * Calls may be nested.
     */
    private void beginTransaction() {
        if (mTransaction == 0) {
            mInitialQuery = mQuery;
            mInitialQueryState = isQueryPending();
        }
        mTransaction++;
    }

    /**
     * Denotes the end of an operation that may change the {@link #mQuery} or
     * {@link #isQueryPending()}.
     * <p>
     * Query and state change callbacks are not dispatched unless the
     * query/state actually changed. For example, if one asynchronous query is
     * stopped but another is started in its place, no state change callback
     * will be sent to listeners.
     * <p>
     * Watch out for early return statements that could prevent this method from
     * being called.
     */
    private void endTransaction() {
        mTransaction--;
        if (mTransaction == 0) {
            if (mQuery != mInitialQuery) {
                dispatchQueryChanged();
            }
            if (mInitialQueryState != isQueryPending()) {
                dispatchQueryStateChanged();
            }
        } else if (mTransaction < 0) {
            throw new IllegalStateException("Mismatched calls to begin/end transaction");
        }
    }

    @Override
    protected void onContentChanged() {
        if (isVisible()) {
            requery();
        }
    }

    public final Context getContext() {
        return mContext;
    }

    private void replaceUri(Uri uri) {
        beginTransaction();
        setQuery(mQuery.replaceUri(uri));
        if (mVisible) {
            startQuery(mQuery);
        }
        endTransaction();
    }

    /** {@inheritDoc} */
    public final boolean hasSolution() {
        return getSolution() != null;
    }

    public final Intent getSolution() {
        Cursor cursor = getCursor();
        return cursor != null ? getSolution(cursor) : null;
    }

    /** {@inheritDoc} */
    public final boolean isReadyToLoadMore() {
        if (isVisible() && hasQuery() && isQueryDone()) {
            Cursor cursor = getCursor();
            if (cursor != null) {
                Bundle extras = cursor.getExtras();
                return !hasError(extras) && hasMore(extras);
            } else {
                // Cursor is not set
                return false;
            }
        } else {
            // Another query is in progress
            return false;
        }
    }

    /** {@inheritDoc} */
    public final boolean isReadyToRefresh() {
        return isVisible() && hasQuery() && isQueryDone() && hasCursor();
    }

    public final boolean isReadyToRequery() {
        // Allow the adapter to requery even if another query is in progress so
        // that the user can make changes to a feed while it is being refreshed.
        return isVisible() && hasQuery();
    }

    /** {@inheritDoc} */
    public final boolean isReadyToRetry() {
        return isVisible() && hasQuery() && isQueryDone() && hasError();
    }

    /** {@inheritDoc} */
    public final boolean loadMore(int amount) {
        if (isReadyToLoadMore()) {
            Cursor cursor = getCursor();
            int n = cursor.getCount() + amount;
            replaceUri(FeedUri.setItemCount(mQuery.getUri(), n));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Refreshes a subset of the items.
     *
     * @param n the number of items to include in the refreshed data. This value
     *            is just a guideline, and the actual number of items loaded may
     *            vary.
     */
    public final boolean refresh(int n) {
        if (isReadyToRefresh()) {
            replaceUri(FeedUri.refresh(mQuery.getUri(), n));
            return true;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public final boolean refresh() {
        if (isReadyToRefresh()) {
            Cursor cursor = getCursor();
            int n = cursor.getCount();
            replaceUri(FeedUri.refresh(mQuery.getUri(), n));
            return true;
        } else {
            return false;
        }
    }

    public final boolean requery() {
        if (isReadyToRequery()) {
            replaceUri(FeedUri.requery(mQuery.getUri()));
            return true;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public final boolean startSolution() {
        Intent solution = getSolution();
        if (solution != null) {
            // Clear the cursor so that it will be requeried
            // automatically when the host activity is resumed.
            clearCursor();

            Context context = getContext();
            context.startActivity(solution);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Clears the {@link Cursor} and stops any pending query.
     * <p>
     * Use this method to requery automatically when the user returns to this
     * {@link Activity} even if it was only paused and not fully stopped.
     */
    public final void clearCursor() {
        beginTransaction();
        changeCursor(null);
        stopQuery();
        endTransaction();
    }

    private void clearQuery() {
        setQuery(null);
    }

    /** {@inheritDoc} */
    public final boolean retry() {
        if (isReadyToRetry()) {
            beginTransaction();
            startQuery(mQuery);
            endTransaction();
            return true;
        } else {
            return false;
        }
    }

    public final void clear() {
        beginTransaction();
        clearCursor();
        clearQuery();
        endTransaction();
    }

    /**
     * Clears the adapter and starts a new query.
     */
    public final void changeQuery(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String orderBy) {
        changeCursor(null);
        replaceQuery(uri, projection, selection, selectionArgs, orderBy);
    }

    /**
     * Changes the query parameters without clearing the adapter.
     */
    public final void replaceQuery(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String orderBy) {
        beginTransaction();
        setQuery(uri, projection, selection, selectionArgs, orderBy);
        if (mVisible) {
            startQuery(mQuery);
        }
        endTransaction();
    }

    /**
     * Register an observer to listen for changes to the query.
     *
     * @see #changeQuery(Uri, String[], String, String[], String)
     * @see #replaceQuery(Uri, String[], String, String[], String)
     */
    public final void registerOnQueryChangeListener(OnQueryChangeListener listener) {
        mQueryObservable.registerObserver(listener);
    }

    /**
     * Unregisters an observer listening for changes to the query.
     */
    public final void unregisterOnQueryChangeListener(OnQueryChangeListener listener) {
        mQueryObservable.unregisterObserver(listener);
    }

    /** {@inheritDoc} */
    public final void registerOnQueryStateChangeListener(FeedAdapter.OnQueryStateChangeListener listener) {
        mQueryStateObservable.registerObserver(listener);
    }

    /** {@inheritDoc} */
    public final void unregisterOnQueryStateChangeListener(FeedAdapter.OnQueryStateChangeListener listener) {
        mQueryStateObservable.unregisterObserver(listener);
    }

    /**
     * Invoked when the query changes or is cleared.
     */
    protected void onQueryChanged() {
    }

    private void dispatchQueryChanged() {
        onQueryChanged();
        mQueryObservable.notifyChanged(this);
    }

    /**
     * Called when a query is started, stopped, or completed (successfully or
     * with an error).
     */
    protected void onQueryStateChanged() {
    }

    private void dispatchQueryStateChanged() {
        onQueryStateChanged();
        mQueryStateObservable.notifyChanged(this);
    }

    public final Uri getUri() {
        return mQuery != null ? mQuery.getUri() : null;
    }

    public final String[] getProjection() {
        return mQuery != null ? mQuery.getProjection() : null;
    }

    public final String getSelection() {
        return mQuery != null ? mQuery.getSelection() : null;
    }

    public final String[] getSelectionArgs() {
        return mQuery != null ? mQuery.getSelectionArgs() : null;
    }

    public final String getOrderBy() {
        return mQuery != null ? mQuery.getOrderBy() : null;
    }

    public final boolean hasQuery() {
        return mQuery != null;
    }

    /** {@inheritDoc} */
    public boolean hasData() {
        return hasCursor();
    }

    public final boolean hasCursor() {
        Cursor cursor = getCursor();
        return cursor != null;
    }

    private void setQuery(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        setQuery(new ContentQuery(uri, projection, selection, selectionArgs, orderBy));
    }

    private void setQuery(ContentQuery query) {
        stopQuery();
        mQuery = query;
        if (mTransaction == 0) {
            throw new IllegalStateException("Query changed outside transaction");
        }
    }

    private void stopQuery() {
        if (mTransaction == 0) {
            throw new IllegalStateException("Query state changed outside transaction");
        }
        if (mTask != null) {
            mTask.cancel(false);
            mTask = null;
        }
    }

    private void startQuery(ContentQuery query) {
        if (mTransaction == 0) {
            throw new IllegalStateException("Query state changed outside transaction");
        }
        mTask = startQueryTask(mQuery);
    }

    /**
     * Starts a task to execute a query asynchronously.
     * <p>
     * Mock adapters can override this method to control the query.
     *
     * @see #completeQuery(Object, Cursor)
     */
    Future<Cursor> startQueryTask(ContentQuery query) {
        QueryTask task = new QueryTask(query);
        task.executeOnThreadPool();
        return task;
    }

    /**
     * Handles the {@link Activity#onResume()} callback.
     * <p>
     * Reloads the {@link Cursor} after a {@link Activity#onStop()} callback.
     */
    public void onResume() {
        beginTransaction();
        mVisible = true;
        if (mQuery != null) {
            Cursor cursor = getCursor();
            if (cursor == null) {
                startQuery(mQuery);
            } else {
                // The Activity was paused, but not stopped, before resuming.
            }
        }
        endTransaction();
    }

    /**
     * Handles the {@link Activity#onStop()} callback.
     */
    public void onStop() {
        beginTransaction();
        mVisible = false;
        if (hasQuery() && hasCursor()) {
            setQuery(mQuery.replaceUri(FeedUri.requery(mQuery.getUri())));
        } else {
            // Use the same content URI instead of a requery URI
            // if the original query never finished.
        }

        // Unload the cursor when it is not visible
        // and stop any pending query.
        clearCursor();
        endTransaction();
    }

    protected final boolean isVisible() {
        return mVisible;
    }

    protected final boolean isDataValid() {
        return mDataValid;
    }

    protected final boolean isDataInvalid() {
        return mDataValid;
    }

    /**
     * Handles the {@link Activity#onSaveInstanceState(Bundle)} callback.
     * <p>
     * Saves the query parameters and {@link AdapterView} state.
     *
     * @param outState the output state.
     */
    public void onSaveInstanceState(Bundle outState) {
        SparseArray<Parcelable> queries = getOrCreateSparseParcelableArray(outState, STATE_QUERIES);
        if (mQuery != null) {
            queries.put(mQueryId, mQuery);
        }
    }

    /**
     * Handles the {@link Activity#onRestoreInstanceState(Bundle)} callback.
     * <p>
     * Restores the query parameters and {@link AdapterView} state.
     *
     * @param state the saved state.
     */
    public void onRestoreInstanceState(Bundle state) {
        beginTransaction();
        SparseArray<Parcelable> queries = state.getSparseParcelableArray(STATE_QUERIES);
        if (queries != null) {
            ContentQuery query = (ContentQuery) queries.get(mQueryId);
            if (query != null) {
                query = query.replaceUri(query.getUri());
                setQuery(query);
            }
        }
        endTransaction();
    }

    public final boolean isQueryPending() {
        return mTask != null;
    }

    /** {@inheritDoc} */
    public final boolean isQueryDone() {
        return mTask == null;
    }

    /**
     * Throws {@link IllegalStateException} if {@link #getCursor()} returns
     * {@code null}.
     */
    private void checkCursorNotNull() {
        Cursor cursor = getCursor();
        if (cursor == null) {
            throw new IllegalStateException("Cursor is null");
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        // The method notifyDataSetChanged should only be called
        // when the cursor is set and not deactivated.
        checkCursorNotNull();
        mDataValid = true;
    }

    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();
        mDataValid = false;
    }

    /** {@inheritDoc} */
    public final boolean isLoadingMore() {
        if (isQueryPending() && hasCursor()) {
            Uri uri = getUri();
            Cursor cursor = getCursor();
            int count = cursor.getCount();

            // Return true if the cursor is not empty and the current number
            // of items is less than the requested number of items.
            return count != 0 && count < FeedUri.getItemCount(uri, 0);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public final boolean hasMore() {
        Cursor cursor = getCursor();
        return cursor != null ? hasMore(cursor) : false;
    }

    /** {@inheritDoc} */
    public final boolean hasError() {
        Cursor cursor = getCursor();
        return cursor != null ? hasError(cursor) : false;
    }

    /** {@inheritDoc} */
    public final int getResponseCode() {
        Cursor cursor = getCursor();
        return cursor != null ? getResponseCode(cursor) : 0;
    }

    /** {@inheritDoc} */
    public final String getResponseMessage() {
        Cursor cursor = getCursor();
        return cursor != null ? getResponseMessage(cursor) : null;
    }

    void completeQuery(Future<Cursor> task, Cursor cursor) {
        beginTransaction();
        if (task.equals(mTask)) {
            mTask = null;
            if (isVisible()) {
                changeCursor(createNonBlockingCursor(cursor));
            } else if (cursor != null) {
                cursor.close();
            }
        } else if (cursor != null) {
            cursor.close();
        }
        endTransaction();
    }

    /**
     * Called when the text filter changed.
     * <p>
     * Implementations should call
     * {@link #replaceQuery(Uri, String[], String, String[], String)} with a new
     * set of query parameters that include the filter text, or do nothing if
     * filtering is not supported.
     * <p>
     * IMPORTANT: The parameter passed to this method may not be consistent with
     * {@link ListView#getTextFilter()}, so always keep a local copy of this
     * value instead of relying on {@link ListView#getTextFilter()}.
     *
     * @param constraint the filter text.
     */
    protected void onFilterChanged(String constraint) {
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new AsyncQueryFilter();
        }
        return mFilter;
    }

    /**
     * Calls {@link BaseFeedAdapter#onFilterChanged(String)} when the
     * {@link AdapterView} specifies a character filter.
     */
    private class AsyncQueryFilter extends Filter implements Handler.Callback {

        private final Handler mMainHandler;

        public AsyncQueryFilter() {
            mMainHandler = new Handler(this);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // Call BaseFeedAdapter#onFilterChanged(CharSequence) on the main thread.
            constraint = constraint != null ? constraint.toString() : null;
            mMainHandler.obtainMessage(0, constraint).sendToTarget();

            // Return an empty result immediately and let AsyncQueryHandler
            // set the result Cursor when the query is complete.
            return new FilterResults();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized boolean handleMessage(Message msg) {
            String constraint = (String) msg.obj;
            onFilterChanged(constraint);
            return true;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // FilterResults is always empty so do nothing
        }
    }

    private class QueryTask extends AsyncTask<Void, Void, Cursor> implements Future<Cursor> {

        private final ContentResolver mResolver;

        private final ContentQuery mContentQuery;

        private boolean mDone;

        public QueryTask(ContentQuery query) {
            Context context = getContext();
            mResolver = context.getContentResolver();
            mContentQuery = query;
        }

        public final android.os.AsyncTask<Void, Void, Cursor> executeOnThreadPool(Void... params) {
            if (Build.VERSION.SDK_INT < 4) {
                // Thread pool size is 1
                return execute(params);
            } else if (Build.VERSION.SDK_INT < 11) {
                // The execute() method uses a thread pool
                return execute(params);
            } else {
                // The execute() method uses a single thread,
                // so call executeOnExecutor() instead.
                try {
                    Method method = android.os.AsyncTask.class.getMethod("executeOnExecutor",
                            Executor.class, Object[].class);
                    Field field = android.os.AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
                    Object executor = field.get(null);
                    method.invoke(this, executor, params);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Unexpected NoSuchMethodException", e);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Unexpected NoSuchFieldException", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unexpected IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Unexpected InvocationTargetException", e);
                }
                return this;
            }
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor cursor = mContentQuery.query(mResolver);
            if (isCancelled()) {
                // The onCancelled(Result) callback might not exist
                // for this platform version so close the Cursor now.
                closeCursor(cursor);
            }
            return cursor;
        }
        
        /** {@inheritDoc} */
        @SuppressWarnings("unused")
        public void onCancelled(Cursor cursor) {
            closeCursor(cursor);
        }
        
        private void closeCursor(Cursor cursor) {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mDone = true;
            completeQuery(this, result);
        }

        /** {@inheritDoc} */
        public boolean isDone() {
            return mDone;
        }
    }
}
