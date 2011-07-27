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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A {@link Parcelable} set of parameters for
 * {@link ContentResolver#query(Uri, String[], String, String[], String)}.
 */
class ContentQuery implements Parcelable {

    public static final Parcelable.Creator<ContentQuery> CREATOR = new Parcelable.Creator<
            ContentQuery>() {
        /**
         * {@inheritDoc}
         */
        public ContentQuery createFromParcel(Parcel in) {
            return new ContentQuery(in);
        }

        /**
         * {@inheritDoc}
         */
        public ContentQuery[] newArray(int size) {
            return new ContentQuery[size];
        }
    };

    /**
     * Writes a {@link String} array that can be read using
     * {@link #readStringArray(Parcel)}.
     */
    private static void writeStringArray(Parcel out, String[] array) {
        if (array != null) {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                out.writeString(array[i]);
            }
        } else {
            out.writeInt(-1);
        }
    }

    /**
     * An easier-to-use alternative to {@link Parcel#readStringArray(String[])}
     * with support for {@code null} arrays.
     */
    private static String[] readStringArray(Parcel in) {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }
        String[] array = new String[length];
        for (int i = 0; i < array.length; i++) {
            array[i] = in.readString();
        }
        return array;
    }

    private final Uri mUri;

    private final String[] mProjection;

    private final String mSelection;

    private final String[] mSelectionArgs;

    private final String mOrderBy;

    public ContentQuery(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        mUri = uri;
        mProjection = projection;
        mSelection = selection;
        mSelectionArgs = selectionArgs;
        mOrderBy = sortOrder;
    }

    private ContentQuery(Parcel in) {
        mUri = in.readParcelable(null);
        mProjection = readStringArray(in);
        mSelection = in.readString();
        mSelectionArgs = readStringArray(in);
        mOrderBy = in.readString();
    }

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mUri, 0);
        writeStringArray(out, mProjection);
        out.writeString(mSelection);
        writeStringArray(out, mSelectionArgs);
        out.writeString(mOrderBy);
    }

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    public ContentQuery replaceUri(Uri uri) {
        String[] projection = mProjection;
        String selection = mSelection;
        String[] selectionArgs = mSelectionArgs;
        String sortOrder = mOrderBy;
        return new ContentQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Executes the query with the given {@link AsyncQueryHandler}, token, and
     * cookie.
     */
    public void startQuery(AsyncQueryHandler queryHandler, int token, Object cookie) {
        queryHandler.startQuery(0, cookie, mUri, mProjection, mSelection, mSelectionArgs, mOrderBy);
    }

    /**
     * Executes the query with the given {@link ContentResolver}.
     */
    public Cursor query(ContentResolver resolver) {
        return resolver.query(mUri, mProjection, mSelection, mSelectionArgs, mOrderBy);
    }

    /**
     * Returns the {@link Uri} passed to the constructor.
     */
    public Uri getUri() {
        return mUri;
    }

    public String[] getProjection() {
        return mProjection;
    }

    public String getSelection() {
        return mSelection;
    }

    public String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    public String getOrderBy() {
        return mOrderBy;
    }
}
