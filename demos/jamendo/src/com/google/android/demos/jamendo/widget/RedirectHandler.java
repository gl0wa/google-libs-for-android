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

package com.google.android.demos.jamendo.widget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Most streaming media players do not know how to handle 307 redirects, so
 * this class follows any redirects before launching the {@link Intent}.
 * <p>
 * The task finishes quickly enough that a progress dialog is not required.
 */
class RedirectHandler extends AsyncTask<Intent, Void, Intent> {
    private final Activity mContext;

    public RedirectHandler(Activity context) {
        mContext = context;
    }

    @Override
    protected Intent doInBackground(Intent... params) {
        if (params == null) {
            throw new NullPointerException();
        }
        if (params.length != 1) {
            throw new IllegalArgumentException();
        }
        try {
            Intent intent = params[0];
            Uri data = intent.getData();
            String type = intent.getType();
            String spec = data.toString();
            URL url = new URL(spec);
            URLConnection connection = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) connection;

            // Disable automatic redirect following
            http.setInstanceFollowRedirects(false);

            int responseCode = http.getResponseCode();
            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    // If there is no redirect, use the unmodified Intent:
                    return intent;
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_MOVED_PERM:
                case 307:
                    // Modify the Intent to use the redirect location:
                    String location = http.getHeaderField("location");
                    if (location == null) {
                        throw new IOException("Missing location header");
                    }
                    intent.setDataAndType(Uri.parse(location), type);
                    return intent;
                default:
                    throw new IOException("Unexpected response code: " + responseCode);

            }
        } catch (IOException e) {
            Log.e(TrackListAdapter.LOG_TAG, "i/o error", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Intent result) {
        if (!mContext.isFinishing() && result != null) {
            startActivitySafely(result);
        }
    }

    private void startActivitySafely(Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent = Intent.createChooser(intent, null);
            mContext.startActivity(intent);
        }
    }
}