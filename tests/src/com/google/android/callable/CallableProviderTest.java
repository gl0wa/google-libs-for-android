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

package com.google.android.callable;

import com.google.android.callable.CallableProviderTest.EchoProvider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.ProviderTestCase2;

public class CallableProviderTest extends ProviderTestCase2<EchoProvider> {

    private static final String AUTHORITY = "com.google.plus.test.echo";

    private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public CallableProviderTest() {
        super(EchoProvider.class, AUTHORITY);
    }

    /**
     * Test method for
     * {@link CallableProvider#call(ContentResolver, Uri, String, String, Bundle)}
     */
    public void testCall() {
        ContentResolver resolver = getMockContentResolver();
        String method = "test";
        String arg = "arg";
        Bundle extras = new Bundle();
        extras.putString("extra-key", "extra-value");
        Bundle result = CallableProvider.call(resolver, AUTHORITY_URI, method, arg, extras);
        assertNotNull(result);
        assertEquals(method, result.getString(EchoProvider.METHOD));
        assertEquals(arg, result.getString(EchoProvider.ARG));
        assertEquals(extras, result.getBundle(EchoProvider.EXTRAS));
    }

    /**
     * Echos values passed as argument to {@link #call(String, String, Bundle)}
     * back to the caller as {@link Bundle} extras.
     */
    public static final class EchoProvider extends ContentProvider implements
            CallableContentProvider {

        public static final String METHOD = "echo_method";
        public static final String ARG = "echo_arg";
        public static final String EXTRAS = "echo_extras";

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Bundle call(String method, String arg, Bundle extras) {
            Bundle bundle = new Bundle();
            bundle.putString(METHOD, method);
            bundle.putString(ARG, arg);
            bundle.putBundle(EXTRAS, extras);
            return bundle;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            return CallableProvider.query(this, uri);
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }
    }
}
