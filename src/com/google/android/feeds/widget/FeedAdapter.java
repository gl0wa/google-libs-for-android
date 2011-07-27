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

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import java.net.HttpURLConnection;

/**
 * Interface for a {@link ListAdapter} or {@link SpinnerAdapter} that can load
 * its own data asynchronously.
 * <p>
 * For example, instead of assigning a {@link Cursor} to a
 * {@link CursorAdapter}, a caller would pass the parameter values supplied to {@link
 * ContentResolver#query(android.net.Uri, String[], String, String[], String)}
 * directly to the adapter and the adapter would perform the query
 * asynchronously on the caller's behalf. Providing the query parameters to the
 * adapter means that it can respond to {@code CursorAdapter#onContentChanged()}
 * by performing an asynchronous query instead of calling
 * {@link Cursor#requery()} synchronously (which could potentially block and
 * make the user interface unresponsive).
 * <p>
 * Some implementations may also be capable of loading additional data
 * asynchronously using {@link #loadMore(int)}.
 * <p>
 * The interface also understands errors (see {@link #hasError()} and solutions
 * to errors that require user interaction (see {@link #startSolution()}).
 */
public interface FeedAdapter extends ListAdapter, SpinnerAdapter {

    /**
     * Listens for changes to the query state.
     *
     * @see FeedAdapter#isQueryDone()
     */
    public interface OnQueryStateChangeListener {
        /**
         * Notifies the listener that the state of an asynchronous query has
         * changed.
         * <p>
         * Unlike {@link DataSetObserver#onChanged()}, this callback may be
         * received before the data set is valid.
         *
         * @param adapter the adapter that changed.
         */
        void onQueryStateChange(FeedAdapter adapter);
    }

    /**
     * Returns {@code true} if the adapter has a solution to the error that can
     * be invoked using {@link #startSolution()}, {@code false} otherwise.
     */
    boolean hasSolution();

    /**
     * Returns {@code true} if the adapter is ready to load more items, {@code
     * false} otherwise.
     */
    boolean isReadyToLoadMore();

    /**
     * Returns {@code true} if the adapter is ready to refresh its contents,
     * {@code false} otherwise.
     */
    boolean isReadyToRefresh();

    /**
     * Returns {@code true} if the adapter is ready to retry the last failed
     * query, {@code false} otherwise.
     */
    boolean isReadyToRetry();

    /**
     * Loads more cursor rows asynchronously.
     *
     * @param amount the number of additional items to load. This value is just
     *            a guideline, and the actual number of items loaded may vary.
     */
    boolean loadMore(int amount);

    /**
     * Refreshes all of the items.
     */
    boolean refresh();

    /**
     * Starts an {@link Intent} to solve a problem.
     */
    boolean startSolution();

    /**
     * Retries the last query asynchronously.
     */
    boolean retry();

    /**
     * Registers an observer to listen for query state changes.
     */
    void registerOnQueryStateChangeListener(FeedAdapter.OnQueryStateChangeListener listener);

    /**
     * Unregisters an observer listening for query state changes.
     */
    void unregisterOnQueryStateChangeListener(FeedAdapter.OnQueryStateChangeListener listener);

    /**
     * Returns {@code true} if the adapter has data (possibly just error info),
     * {@code false} otherwise.
     */
    boolean hasData();

    /**
     * Returns {@code true} if there are no asynchronous queries pending, {@code
     * false} otherwise.
     */
    boolean isQueryDone();

    /**
     * Returns {@code true} if there is an asynchronous query pending and it is
     * loading additional rows (as opposed to just refreshing existing rows),
     * {@code false} otherwise.
     */
    boolean isLoadingMore();

    /**
     * Returns {@code true} if there might be more data to load, {@code false}
     * otherwise.
     */
    boolean hasMore();

    /**
     * Returns {@code true} if there was an error, {@code false} otherwise.
     */
    boolean hasError();

    /**
     * Returns a status code describing the data that was loaded. For example,
     * {@link HttpURLConnection#HTTP_OK},
     * {@link HttpURLConnection#HTTP_UNAUTHORIZED},
     * {@link HttpURLConnection#HTTP_INTERNAL_ERROR}, or
     * {@link HttpURLConnection#HTTP_NOT_FOUND}.
     * <p>
     * Although the underlying transport may or may not be HTTP, these values
     * are used as a base set for describing common types of errors.
     * <p>
     * The return value is undefined when {@link #hasData()} is {@code false}.
     */
    int getResponseCode();

    /**
     * Returns a message describing the data that was loaded.
     * <p>
     * This message may no be localized, so it is generally recommended that
     * callers map {@link #getResponseCode()} to a localized message and only
     * use this value for debugging.
     * <p>
     * The return value is undefined when {@link #hasData()} is {@code false}.
     */
    String getResponseMessage();
}
