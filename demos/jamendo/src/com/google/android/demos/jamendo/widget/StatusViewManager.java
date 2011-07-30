/*-
 * Copyright (C) 2011 Google Inc.
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

package com.google.android.demos.jamendo.widget;

import com.google.android.demos.jamendo.R;
import com.google.android.feeds.FeedExtras;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;

/**
 * Shows a "Loading..." message while a list is loading and a indeterminate
 * progress spinner in the title bar when something is loading in the
 * background, or an error message if there is a problem loading the data.
 */
public final class StatusViewManager implements LoaderManager.LoaderCallbacks<Cursor> {

    private final LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private final int mLoaderId;

    private final Window mWindow;

    private final AdapterView<?> mAdapterView;

    private final View mLoading;

    private final View mError;

    private final SparseBooleanArray mActive = new SparseBooleanArray();

    /**
     * Decorates {@link LoaderManager.LoaderCallbacks} in order to show loading
     * and error indicator views.
     * 
     * @param callbacks the callbacks to decorate.
     * @param id the loader ID of the primary data.
     * @param activity an activity containing the loading and error views. If
     *            the {@link Window} has
     *            {@link Window#FEATURE_INDETERMINATE_PROGRESS}, a spinner will
     *            when the primary loader is reloading data or when a secondary
     *            loader is running.
     * @param listener a listener for retry button clicks.
     */
    public StatusViewManager(LoaderManager.LoaderCallbacks<Cursor> callbacks, int id,
            FragmentActivity activity, View.OnClickListener listener) {
        mCallbacks = callbacks;
        mLoaderId = id;
        mWindow = activity.getWindow();
        mAdapterView = (AdapterView<?>) activity.findViewById(android.R.id.list);
        mLoading = activity.findViewById(R.id.loading);
        mError = activity.findViewById(R.id.error);
        mError.findViewById(R.id.retry).setOnClickListener(listener);

        // Loader may still be running from last Activity instance
        mLoading.setVisibility(View.VISIBLE);
        mError.setVisibility(View.GONE);
    }

    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mActive.put(id, true);
        if (mLoaderId == id) {
            mLoading.setVisibility(isEmpty() ? View.VISIBLE : View.GONE);
            mError.setVisibility(View.GONE);
        }
        updateWindowIndeterminateProgress();
        return mCallbacks.onCreateLoader(id, args);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCallbacks.onLoadFinished(loader, data);
        mActive.delete(loader.getId());
        if (mLoaderId == loader.getId()) {
            mLoading.setVisibility(View.GONE);
            mError.setVisibility(isEmpty() && hasError(data) ? View.VISIBLE : View.GONE);
        }
        updateWindowIndeterminateProgress();
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Cursor> loader) {
        mCallbacks.onLoaderReset(loader);
        mActive.delete(loader.getId());
        if (mLoaderId == loader.getId()) {
            mLoading.setVisibility(View.GONE);
            mError.setVisibility(View.GONE);
        }
        updateWindowIndeterminateProgress();
    }

    private void updateWindowIndeterminateProgress() {
        // Show an indeterminate progress spinner when something is loading,
        // unless the main loading view is already visible.
        mWindow.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, mActive.size() != 0
                && mLoading.getVisibility() != View.VISIBLE ? Window.PROGRESS_VISIBILITY_ON
                : Window.PROGRESS_VISIBILITY_OFF);
    }

    private boolean isEmpty() {
        // Don't use AdapterView#getCount() because it is updated asynchronously
        Adapter adapter = mAdapterView.getAdapter();
        return adapter == null || adapter.isEmpty();
    }

    private static boolean hasError(Cursor data) {
        return data == null || data.getExtras().containsKey(FeedExtras.EXTRA_ERROR);
    }
}
