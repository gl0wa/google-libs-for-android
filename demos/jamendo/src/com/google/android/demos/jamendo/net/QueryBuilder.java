/*-
 * Copyright (C) 2009 Google Inc.
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

package com.google.android.demos.jamendo.net;

import android.net.Uri;

import java.util.List;

/**
 * A class similar to {@link Uri.Builder} that builds URI queries instead of
 * full URIs.
 */
public class QueryBuilder {

    private static final Uri BASE = Uri.parse("scheme://authority");

    private final Uri.Builder mQuery;

    public QueryBuilder() {
        this(null);
    }

    public QueryBuilder(String query) {
        mQuery = BASE.buildUpon();
        if (query != null) {
            mQuery.encodedQuery(query);
        }
    }

    public void append(String key, String value) {
        mQuery.appendQueryParameter(key, value);
    }

    public List<String> getQueryParameters(String key) {
        Uri uri = mQuery.build();
        return uri.getQueryParameters(key);
    }

    public String getQueryParameter(String key) {
        Uri uri = mQuery.build();
        return uri.getQueryParameter(key);
    }

    public String build() {
        Uri uri = mQuery.build();
        return uri.getEncodedQuery();
    }

    @Override
    public String toString() {
        return build();
    }
}
