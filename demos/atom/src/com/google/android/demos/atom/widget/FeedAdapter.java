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

package com.google.android.demos.atom.widget;

import com.google.android.demos.atom.provider.AtomContract.Entries;
import com.google.android.demos.atom.provider.AtomContract.Feeds;
import com.google.android.feeds.FeedExtras;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Adapter for an Atom feed.
 */
public class FeedAdapter extends CursorAdapter {
    private static final String[] PROJECTION = {
            Entries._ID, Entries.TITLE_PLAINTEXT, Entries.SUMMARY, Entries.CONTENT,
            Entries.ALTERNATE_HREF
    };

    public static Loader<Cursor> createLoader(Context context, Uri uri) {
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    private static final int COLUMN_TITLE = 1;

    private static final int COLUMN_SUMMARY = 2;

    private static final int COLUMN_CONTENT = 3;

    private static final int COLUMN_ALTERNATE_HREF = 4;

    public static void setEntryData(EntryDialog entryDialog, Cursor cursor) {
        Bundle extras = cursor.getExtras();
        String title = cursor.getString(COLUMN_TITLE);
        String url = cursor.getString(COLUMN_ALTERNATE_HREF);
        String summary = cursor.getString(COLUMN_SUMMARY);
        String content = cursor.getString(COLUMN_CONTENT);
        String feed = extras.getString(Feeds.TITLE_PLAINTEXT);
        entryDialog.setData(title, url, summary, content, feed);
    }

    public FeedAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String title = cursor.getString(COLUMN_TITLE);
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        text1.setText(title);
    }
    
    public boolean hasError() {
        Cursor cursor = getCursor();
        return cursor != null && cursor.getExtras().containsKey(FeedExtras.EXTRA_ERROR);
    }
}
