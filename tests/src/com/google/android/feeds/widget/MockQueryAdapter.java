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
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class MockQueryAdapter extends BaseFeedAdapter {

    private Future<Cursor> mTask;

    public MockQueryAdapter(Context context, int id) {
        super(context, id);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    Future<Cursor> startQueryTask(ContentQuery query) {
        // The asynchronous task must be completed manually by the test
        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();
        mTask = new MockQueryTask(resolver, query);
        return mTask;
    }

    /**
     * Executes the pending query, but does not deliver the result.
     */
    Cursor executeQuery() throws ExecutionException, InterruptedException {
        if (mTask == null) {
            throw new IllegalStateException("No query to complete");
        }
        try {
            return mTask.get();
        } catch (CancellationException e) {
            return null;
        }
    }

    /**
     * Completes the query that was started most recently and delivers the
     * result to the adapter.
     */
    void completeQuery() throws ExecutionException, InterruptedException {
        Cursor cursor = executeQuery();
        completeQuery(mTask, cursor);
        mTask = null;
    }

    private static class MockQueryTask implements Future<Cursor> {

        private final ContentResolver mContentResolver;

        private final ContentQuery mContentQuery;

        private boolean mCancelled;

        private boolean mDone;

        private Cursor mCursor;

        public MockQueryTask(ContentResolver contentResolver, ContentQuery contentQuery) {
            mContentResolver = contentResolver;
            mContentQuery = contentQuery;
        }

        /**
         * {@inheritDoc}
         */
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!mCancelled && !mDone) {
                mCancelled = true;
                if (mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Cursor get() throws ExecutionException {
            if (mCancelled) {
                throw new CancellationException();
            }
            if (!mDone) {
                try {
                    mCursor = mContentQuery.query(mContentResolver);
                } catch (Throwable t) {
                    throw new ExecutionException(t);
                } finally {
                    mDone = true;
                }
            }
            return mCursor;
        }

        /**
         * {@inheritDoc}
         */
        public Cursor get(long timeout, TimeUnit unit) throws ExecutionException {
            return get();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCancelled() {
            return mCancelled;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDone() {
            return mDone;
        }
    }
}
