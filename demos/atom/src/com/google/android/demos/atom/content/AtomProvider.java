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

package com.google.android.demos.atom.content;

import com.google.android.demos.atom.provider.AtomContract.Entries;
import com.google.android.feeds.content.FeedLoader;
import com.google.android.feeds.content.FeedProvider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Provides a {@link Cursor} interface to an Atom feed.
 */
public class AtomProvider extends ContentProvider {

    private UriMatcher mUriMatcher;

    private static final int FEED = 1;

    private static final int ENTRY = 2;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(info.authority, "feeds/*/entries", FEED);
        mUriMatcher.addURI(info.authority, "feeds/*/entries/*", ENTRY);
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case FEED:
                return Entries.CONTENT_TYPE;
            case ENTRY:
                return Entries.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(projection);
        Bundle extras = new Bundle();
        try {
            // Create a handler that will listen for XML events,
            // translate them into rows, and append the rows 
            // to the output cursor.
            AtomContentHandler handler = new AtomContentHandler(cursor, extras);

            if (ENTRY == mUriMatcher.match(uri)) {
                // For feed entry URIs, filter all entries in the feed except
                // for
                // the entry specified in the content:// URI.
                String id = Entries.getEntryId(uri);
                handler.setFilter(id);
            }

            // Create an HTTP query to fetch the document containing the XML.
            String feedUrl = Entries.getFeedUrl(uri);
            Uri feedUri = Uri.parse(feedUrl);
            FeedLoader.loadFeed(handler, feedUri);
            return FeedProvider.feedCursor(cursor, extras);
        } catch (Throwable t) {
            return FeedProvider.errorCursor(cursor, extras, t);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
