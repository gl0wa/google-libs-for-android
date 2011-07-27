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

import com.google.android.feeds.content.FeedProvider;
import com.google.android.feeds.content.UnauthorizedException;
import com.google.android.feeds.provider.FeedContract;
import com.google.android.feeds.provider.FeedUri;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.test.MoreAsserts;
import android.test.ProviderTestCase2;

/**
 * Test case for {@link BaseFeedAdapter}.
 */
public class BaseFeedAdapterTest extends ProviderTestCase2<MockQueryProvider> {

    private static final String AUTHORITY = "com.google.android.feeds.test";

    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final Intent SOLUTION = new Intent();

    private static final int QUERY_ID = 1;

    private static final String[] PROJECTION = {
        BaseColumns._ID
    };

    private static final String SELECTION = null;

    private static final String[] SELECTION_ARGS = null;

    private static final String SORT_ORDER = null;

    private static Cursor createCursor(int rowCount) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION);
        for (int position = 0; position < rowCount; position++) {
            MatrixCursor.RowBuilder row = cursor.newRow();
            for (int columnIndex = 0; columnIndex < PROJECTION.length; columnIndex++) {
                row.add(Long.valueOf(position));
            }
        }
        return cursor;
    }

    private static Cursor createPartialCursor(Cursor cursor) {
        Bundle extras = new Bundle();
        extras.putBoolean(FeedContract.EXTRA_MORE, true);
        return FeedProvider.feedCursor(cursor, extras);
    }

    private static Cursor createErrorCursor() {
        Cursor cursor = createCursor(0);
        Bundle extras = new Bundle();
        Throwable t = new RuntimeException("Mock error");
        return FeedProvider.errorCursor(cursor, extras, t);
    }

    private static Cursor createUnauthorizedCursor() {
        Cursor cursor = createCursor(0);
        Bundle extras = new Bundle();
        Throwable t = new UnauthorizedException("Unauthorized", SOLUTION);
        return FeedProvider.errorCursor(cursor, extras, t);
    }

    private static Uri createQuantifiedUri(int n) {
        return FeedUri.setItemCount(CONTENT_URI, n);
    }

    private static Uri createRefreshUri(int n) {
        return FeedUri.refresh(CONTENT_URI, n);
    }

    private MockContext mContext;

    private MockQueryProvider mProvider;

    private MockQueryAdapter mAdapter;

    private MockOnQueryStateChangeListener mOnQueryStateChangeListener;

    public BaseFeedAdapterTest() {
        super(MockQueryProvider.class, AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = new MockContext(getMockContext());
        mProvider = getProvider();
        mAdapter = new MockQueryAdapter(mContext, QUERY_ID);

        mOnQueryStateChangeListener = new MockOnQueryStateChangeListener(mAdapter);
        mAdapter.registerOnQueryStateChangeListener(mOnQueryStateChangeListener);
    }

    @Override
    protected void tearDown() throws Exception {
        mAdapter.unregisterOnQueryStateChangeListener(mOnQueryStateChangeListener);
        mOnQueryStateChangeListener = null;
        mAdapter = null;
        mProvider = null;
        mContext = null;
        super.tearDown();
    }

    private void executeQuery() throws Exception {
        mAdapter.executeQuery();
        mProvider.replay();
    }

    private void completeQuery() throws Exception {
        mAdapter.completeQuery();
        mProvider.replay();
    }

    private void checkAdapterHasPendingQuery() {
        assertTrue(mAdapter.isQueryPending());
        assertFalse(mAdapter.isQueryDone());
    }

    private void checkAdapterLoadingMore() {
        assertTrue(mAdapter.isLoadingMore());
    }

    private void checkAdapterNotLoadingMore() {
        assertFalse(mAdapter.isLoadingMore());
    }

    /**
     * Checks if the cursor managed by {@link #mAdapter} has an error.
     * 
     * @param expected a cursor with the expected error.
     * @return the actual cursor specifying with an error.
     */
    private Cursor checkAdapterHasError(Cursor expected) {
        Cursor actual = mAdapter.getCursor();
        assertNotNull(actual);
        assertTrue(mAdapter.hasError());
        
        // Bundle does not implement Object#equals(Object)
        // so perform a String comparison instead.
        assertEquals(String.valueOf(expected.getExtras()), String.valueOf(actual.getExtras()));
        
        return actual;
    }

    /**
     * Checks if {@link #mAdapter} has the expected data.
     * 
     * @param expected the expected cursor.
     * @return the actual cursor.
     */
    private Cursor checkAdapterHasData(Cursor expected) {
        Cursor actual = mAdapter.getCursor();
        assertNotNull(actual);

        assertTrue(mAdapter.isDataValid());
        assertEquals(expected.getCount(), mAdapter.getCount());

        String[] expectedProjection = expected.getColumnNames();
        String[] actualProjection = actual.getColumnNames();
        MoreAsserts.assertEquals(expectedProjection, actualProjection);

        int columnIndex = expected.getColumnIndexOrThrow(BaseColumns._ID);

        for (int position = 0; expected.moveToPosition(position); position++) {
            long expectedId = expected.getLong(columnIndex);
            long actualId = mAdapter.getItemId(position);
            assertEquals(expectedId, actualId);
        }
        return actual;
    }

    private void checkAdapterIsStopped() {
        assertNull(mAdapter.getCursor());
        assertFalse(mAdapter.isDataValid());
        assertFalse(mAdapter.isQueryPending());
    }

    private void checkAdapterQueryIsDone() {
        assertTrue(mAdapter.isQueryDone());
        assertFalse(mAdapter.isLoadingMore());
        assertFalse(mAdapter.isQueryPending());
    }

    private void checkClosed(Cursor cursor) {
        assertTrue(cursor.isClosed());
    }

    public void testNoCursorNoQuery() {
        mAdapter.onResume();
        mAdapter.onStop();
    }

    public void testCursorNoQuery() {
        Cursor cursor = createCursor(10);
        mAdapter.changeCursor(cursor);
        mAdapter.onResume();
        mAdapter.onStop();
        checkClosed(cursor);
    }

    public void testStop() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // Complete the query
        Cursor cursor1 = createCursor(10);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();

        // Stop the Activity
        mAdapter.onStop();
        checkAdapterIsStopped();
        assertTrue(cursor1.isClosed());

        // Resume the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the requery
        Cursor cursor2 = createCursor(5);
        mProvider.expectQuery(FeedUri.requery(CONTENT_URI)).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        assertTrue(cursor2.isClosed());
    }

    public void testStopBeforeQueryComplete() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Execute the query, but do not deliver the result to the adapter (yet)
        Cursor cursor1 = createCursor(10);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        executeQuery();

        // Stop the Activity
        mAdapter.onStop();
        checkAdapterIsStopped();

        // Complete the query while the activity is stopped
        // (the result should be ignored by the adapter)
        completeQuery();
        checkAdapterIsStopped();
        checkClosed(cursor1);

        // Resume the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the query (not a requery!)
        Cursor cursor2 = createCursor(15);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
    }

    public void testRequeryOnContentChanged() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the query
        Cursor cursor1 = createCursor(10);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();

        // Send content change notification
        assertTrue(mAdapter.isReadyToRequery());
        mAdapter.onContentChanged();
        checkAdapterHasData(cursor1);
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // Complete the requery
        Cursor cursor2 = createCursor(5);
        mProvider.expectQuery(FeedUri.requery(CONTENT_URI)).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkClosed(cursor1);

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
    }

    public void testRefresh() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the query
        Cursor cursor1 = createCursor(10);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();

        // Refresh the data
        assertTrue(mAdapter.isReadyToRefresh());
        assertTrue(mAdapter.refresh());
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // Check if the data is still valid
        // while the refresh is in progress.
        checkAdapterHasData(cursor1);

        // Complete the refresh
        Cursor cursor2 = createCursor(15);
        mProvider.expectQuery(createRefreshUri(10)).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();
        checkClosed(cursor1);

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
    }

    public void testRetryAfterError() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the query
        Cursor cursor1 = createErrorCursor();
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasError(cursor1);

        // Retry the failed query
        assertTrue(mAdapter.isReadyToRetry());
        assertTrue(mAdapter.retry());
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // Complete the retry
        Cursor cursor2 = createCursor(15);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();
        checkClosed(cursor1);

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
    }

    public void testLoadMore() throws Exception {
        Uri contentUri = createQuantifiedUri(10);
        mAdapter.changeQuery(contentUri, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // Complete the query
        Cursor cursor1 = createPartialCursor(createCursor(10));
        mProvider.expectQuery(contentUri).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();

        // Load more rows
        assertTrue(mAdapter.hasMore());
        assertTrue(mAdapter.isReadyToLoadMore());
        assertTrue(mAdapter.loadMore(15));
        checkAdapterHasPendingQuery();
        checkAdapterLoadingMore();

        // Check if the data is still valid
        // while the additional rows are loading
        checkAdapterHasData(cursor1);

        // Complete the retry
        Cursor cursor2 = createCursor(25);
        mProvider.expectQuery(createQuantifiedUri(25)).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();
        checkClosed(cursor1);

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
    }

    public void testSolution() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();

        // Complete the query
        Cursor cursor1 = createUnauthorizedCursor();
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();
        assertTrue(mAdapter.hasError());
        assertTrue(mAdapter.hasSolution());

        // Launch the Intent
        mContext.expectStartActivity(SOLUTION);
        mAdapter.startSolution();
        mContext.replay();

        // The current implementation clears the
        // Cursor to force a requery in onResume.
        assertNull(mAdapter.getCursor());
        assertTrue(cursor1.isClosed());

        // The activity will be paused, but it may not be stopped
        // if the launched Intent is a dialog

        // Resume the Activity and ensure that the requery happened
        // even though the activity was not fully stopped.
        mAdapter.onResume();
        checkAdapterHasPendingQuery();
        Cursor cursor2 = createCursor(5);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkAdapterQueryIsDone();

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        assertTrue(cursor2.isClosed());
    }

    public void testOnQueryStateChange() throws Exception {
        mAdapter.changeQuery(CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
        assertEquals(0, mOnQueryStateChangeListener.getCallbackCount());

        // Start the Activity
        mAdapter.onResume();
        checkAdapterHasPendingQuery();
        assertEquals(1, mOnQueryStateChangeListener.getCallbackCount());

        // Complete the query
        Cursor cursor1 = createCursor(10);
        mProvider.expectQuery(CONTENT_URI).andReturn(cursor1);
        completeQuery();
        cursor1 = checkAdapterHasData(cursor1);
        checkAdapterQueryIsDone();
        assertEquals(2, mOnQueryStateChangeListener.getCallbackCount());

        // Requery
        assertTrue(mAdapter.isReadyToRequery());
        assertTrue(mAdapter.requery());
        checkAdapterHasData(cursor1);
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();
        assertEquals(3, mOnQueryStateChangeListener.getCallbackCount());

        // Requery again before completing the first requery
        assertTrue(mAdapter.isReadyToRequery());
        assertTrue(mAdapter.requery());
        checkAdapterHasData(cursor1);
        checkAdapterHasPendingQuery();
        checkAdapterNotLoadingMore();

        // The second requery should not trigger a state change
        // because the adapter was already requerying.
        assertEquals(3, mOnQueryStateChangeListener.getCallbackCount());

        // Complete the requery
        Cursor cursor2 = createCursor(5);
        mProvider.expectQuery(FeedUri.requery(CONTENT_URI)).andReturn(cursor2);
        completeQuery();
        cursor2 = checkAdapterHasData(cursor2);
        checkClosed(cursor1);
        assertEquals(4, mOnQueryStateChangeListener.getCallbackCount());

        // Clean-up
        mAdapter.onStop();
        checkAdapterIsStopped();
        checkClosed(cursor2);
        assertEquals(4, mOnQueryStateChangeListener.getCallbackCount());
    }
}
