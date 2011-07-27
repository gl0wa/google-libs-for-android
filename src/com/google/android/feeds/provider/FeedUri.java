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

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.net.UrlQuerySanitizer.ParameterValuePair;
import android.net.UrlQuerySanitizer.ValueSanitizer;

/**
 * Methods for creating and parsing feed URIs.
 *
 * @see FeedContract#PARAM_ITEM_COUNT
 * @see FeedContract#PARAM_MAX_AGE
 */
public class FeedUri {

    private static final UrlQuerySanitizer sUrlQueryParser = createUrlQueryParser();

    /**
     * Creates a URL query parser by disabling all sanitizing features of a
     * {@link UrlQuerySanitizer}.
     * <p>
     * {@link UrlQuerySanitizer} is the only API in the Android standard library
     * that can enumerate the full set of query parameters for a {@link Uri}.
     */
    private static UrlQuerySanitizer createUrlQueryParser() {
        UrlQuerySanitizer sanitizer = new UnicodeUrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        ValueSanitizer valueSanitizer = UrlQuerySanitizer.getAllButNulLegal();
        sanitizer.setUnregisteredParameterValueSanitizer(valueSanitizer);
        return sanitizer;
    }

    private static class UnicodeUrlQuerySanitizer extends UrlQuerySanitizer {
        @Override
        public String unescape(String string) {
            // See: http://code.google.com/p/android/issues/detail?id=14437
            return Uri.decode(string);
        }
    }

    /**
     * Replaces all instances of the given query parameter with a single
     * instance of the query parameter having the given value.
     *
     * @param uri the input {@link Uri}
     * @param parameter the parameter to replace
     * @param value the new parameter value.
     * @return the output {@link Uri}.
     */
    static Uri replaceQueryParameter(Uri uri, String parameter, String value) {
        UrlQuerySanitizer parser = sUrlQueryParser;
        synchronized (parser) {
            String url = uri.toString();
            parser.parseUrl(url);

            Uri.Builder builder = uri.buildUpon();

            // Clear the existing query
            builder.query("");

            // Re-append all the parameters except for those to be replaced
            for (ParameterValuePair pair : parser.getParameterList()) {
                if (!parameter.equals(pair.mParameter)) {
                    builder.appendQueryParameter(pair.mParameter, pair.mValue);
                }
            }

            // Append the replacement parameter
            builder.appendQueryParameter(parameter, value);

            return builder.build();
        }
    }

    /**
     * Creates a {@link Builder} with {@link FeedContract#PARAM_ITEM_COUNT} and
     * {@link FeedContract#PARAM_MAX_AGE} removed.
     */
    static Uri.Builder removeFeedParams(Uri uri) {
        UrlQuerySanitizer parser = sUrlQueryParser;
        synchronized (parser) {
            String url = uri.toString();
            parser.parseUrl(url);

            Uri.Builder builder = uri.buildUpon();

            // Clear the existing query
            builder.query("");

            // Re-append all the parameters except for those to be replaced
            for (ParameterValuePair pair : parser.getParameterList()) {
                if (!FeedContract.PARAM_ITEM_COUNT.equals(pair.mParameter)
                        && !FeedContract.PARAM_MAX_AGE.equals(pair.mParameter)) {
                    builder.appendQueryParameter(pair.mParameter, pair.mValue);
                }
            }

            return builder;
        }
    }
    
    static Uri createFeedUri(Uri uri, int n, long maxAge) {
        Uri.Builder builder = removeFeedParams(uri);

        // Append the replacement parameters
        builder.appendQueryParameter(FeedContract.PARAM_ITEM_COUNT, Integer.toString(n));
        builder.appendQueryParameter(FeedContract.PARAM_MAX_AGE, Long.toString(maxAge));

        return builder.build();
    }

    public static Uri setMaxAge(Uri uri, long maxAge) {
        return replaceQueryParameter(uri, FeedContract.PARAM_MAX_AGE, Long.toString(maxAge));
    }

    public static long getMaxAge(Uri uri, long defaultValue) {
        String maxAge = uri.getQueryParameter(FeedContract.PARAM_MAX_AGE);
        if (maxAge != null) {
            return Long.parseLong(maxAge);
        } else {
            return defaultValue;
        }
    }

    public static int getItemCount(Uri uri, int defaultValue) {
        String count = uri.getQueryParameter(FeedContract.PARAM_ITEM_COUNT);
        if (count != null) {
            return Integer.parseInt(count);
        } else {
            return defaultValue;
        }
    }

    public static Uri setItemCount(Uri uri, int n) {
        String itemCount = Integer.toString(n);
        return replaceQueryParameter(uri, FeedContract.PARAM_ITEM_COUNT, itemCount);
    }

    public static Uri refresh(Uri uri) {
        return setMaxAge(uri, 0L);
    }

    public static Uri refresh(Uri uri, int n) {
        return createFeedUri(uri, n, 0L);
    }

    public static Uri requery(Uri uri) {
        return createFeedUri(uri, 0, Long.MAX_VALUE);
    }

    /**
     * Removes query parameters set by {@link #setItemCount(Uri, int)} and
     * {@link #setMaxAge(Uri, long)}.
     */
    public static Uri normalize(Uri uri) {
        return removeFeedParams(uri).build();
    }

    protected FeedUri() {
        // The constructor is protected so that a contract class can
        // extend this class to inherit its method definitions.
    }
}
