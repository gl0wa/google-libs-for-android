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

package com.google.android.feeds.provider;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.net.HttpURLConnection;

/**
 * Defines content URI query parameters and cursor extras.
 */
public interface FeedContract {

    /**
     * Specifies the maximum age for cached content used to generate the result
     * set.
     *
     * @see android.net.Uri.Builder#appendQueryParameter(String, String)
     * @see ContentResolver#query(Uri, String[], String, String[], String)
     */
    String PARAM_MAX_AGE = "max-age";

    /**
     * Specifies the desired number of items.
     * <p>
     * The {@link ContentProvider} may return more or fewer items, depending on
     * availability and efficiency considerations.
     *
     * @see android.net.Uri.Builder#appendQueryParameter(String, String)
     * @see ContentResolver#query(Uri, String[], String, String[], String)
     */
    String PARAM_ITEM_COUNT = "n";

    /**
     * An HTTP response code describing the last response.
     * <p>
     * The supplied value may or may not originate from an HTTP server. For
     * example, {@link HttpURLConnection#HTTP_BAD_GATEWAY} may be specified if
     * the device does not have a connection, or
     * {@link HttpURLConnection#HTTP_UNAUTHORIZED} may be specified if the
     * application needs to ask for permission to use an authentication token
     * provided by the {@link android.accounts.AccountManager}.
     *
     * @see HttpURLConnection#getResponseCode()
     * @see Cursor#getExtras()
     * @see Bundle#getInt(String)
     */
    String EXTRA_RESPONSE_CODE = "com.google.android.feeds.cursor.extra.RESPONSE_CODE";

    /**
     * An HTTP response message describing the last response.
     * <p>
     * The supplied value may or may not originate from an HTTP server and may
     * or may not be localized.
     *
     * @see HttpURLConnection#getResponseMessage()
     * @see Cursor#getExtras()
     * @see Bundle#getInt(String)
     */
    String EXTRA_RESPONSE_MESSAGE = "com.google.android.feeds.cursor.extra.RESPONSE_MESSAGE";

    /**
     * The local cache timestamp for the {@link Cursor} data.
     * <p>
     * Applications can use this value to decide if the cursor data should be
     * refreshed asynchronously.
     *
     * @see Cursor#getExtras()
     * @see Bundle#getLong(String)
     */
    String EXTRA_TIMESTAMP = "com.google.android.feeds.cursor.extra.TIMESTAMP";

    /**
     * Boolean extra indicating if more rows may be available.
     *
     * @see Cursor#getExtras()
     * @see Bundle#getBoolean(String)
     */
    String EXTRA_MORE = "com.google.android.feeds.cursor.extra.MORE";

    /**
     * An {@link Intent} that can be used to solve a problem.
     * <p>
     * For example, the {@link Intent} may start an {@link Activity} to re-enter
     * a password after it was changed remotely.
     *
     * @see Cursor#getExtras()
     * @see Bundle#getParcelable(String)
     * @see Context#startActivity(Intent)
     */
    String EXTRA_SOLUTION = "com.google.android.feeds.cursor.extra.SOLUTION";
}
