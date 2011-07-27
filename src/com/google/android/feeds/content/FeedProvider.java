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

package com.google.android.feeds.content;

import com.google.android.feeds.provider.FeedContract;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.os.Bundle;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Creates {@link Cursor} wrappers that include error information and other
 * meta-data.
 */
public class FeedProvider {

    /**
     * Ensures that the query has been evaluated.
     * <p>
     * Some versions of the Android platform do not evaluate the query until the
     * {@link Cursor} is used. This method helps ensure that the query is
     * evaluated on a background thread and not the main thread.
     */
    private static Cursor evaluate(Cursor cursor) {
        if (cursor != null) {
            cursor.getCount();
        }
        return cursor;
    }

    private static AnnotatedCursor createAnnotatedCursor(Cursor cursor, Bundle extras) {
        if (cursor instanceof CrossProcessCursor) {
            CrossProcessCursor crossProcessCursor = (CrossProcessCursor) cursor;
            return new AnnotatedCrossProcessCursor(crossProcessCursor, extras);
        } else if (cursor != null) {
            return new AnnotatedCursor(cursor, extras);
        } else {
            return null;
        }
    }

    /**
     * Creates a cursor with meta-data.
     *
     * @param cursor the {@link Cursor} to wrap.
     * @param extras meta-data, such as {@link FeedContract#EXTRA_MORE}.
     * @return the wrapped cursor.
     */
    public static Cursor feedCursor(Cursor cursor, Bundle extras) {
        return createAnnotatedCursor(evaluate(cursor), extras);
    }

    /**
     * Creates a cursor with error data.
     *
     * @param cursor the cursor to wrap. When there is an error, the provider
     *            should generally return any data that is cached locally. For
     *            example, if the first 10 items are available locally, it is
     *            usually best to show them to the user even if the next 10
     *            items are unavailable.
     * @param extras meta-data, such as {@link FeedContract#EXTRA_MORE}. The
     *            flag {@link FeedContract#EXTRA_MORE} should be set if there
     *            are more items, even if there was an error so that the UI can
     *            retry the operation to load more items after the network
     *            connection has been restored.
     * @param t the exception that was thrown. The calling code should catch
     *            {@link Throwable} in {@link
     *            ContentProvider#query(android.net.Uri, String[], String,
     *            String[], String)} otherwise {@link
     *            ContentResolver#query(android.net.Uri, String[], String,
     *            String[], String)} will return {@code null} by default.
     * @return the wrapped cursor.
     * @see UnauthorizedException
     */
    public static Cursor errorCursor(Cursor cursor, Bundle extras, Throwable t) {
        int responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        String responseMessage = t.getMessage();
        Intent solution = null;
        if (t instanceof UnauthorizedException) {
            UnauthorizedException e = (UnauthorizedException) t;
            responseCode = HttpURLConnection.HTTP_UNAUTHORIZED;
            solution = e.getSolution();
        } else if (t instanceof IOException) {
            responseCode = HttpURLConnection.HTTP_BAD_GATEWAY;
        }
        if (responseMessage == null) {
            // Use the exception class name as an error message
            Class<? extends Throwable> type = t.getClass();
            responseMessage = type.getSimpleName();
        }
        extras.putInt(FeedContract.EXTRA_RESPONSE_CODE, responseCode);
        extras.putString(FeedContract.EXTRA_RESPONSE_MESSAGE, responseMessage);
        if (solution != null) {
            extras.putParcelable(FeedContract.EXTRA_SOLUTION, solution);
        }
        return feedCursor(cursor, extras);
    }

    private FeedProvider() {
    }
}
