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

package com.google.android.demos.rss.widget;

import com.google.android.demos.rss.provider.RssContract.Channels;
import com.google.android.demos.rss.provider.RssContract.Items;
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
 * Adapter for an RSS channel.
 */
public class ChannelAdapter extends CursorAdapter {

    private static final String[] PROJECTION = {
            Items._ID, Items.TITLE_PLAINTEXT, Items.DESCRIPTION, Items.LINK
    };

    public static Loader<Cursor> createLoader(Context context, Uri uri) {
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    private static final int COLUMN_TITLE = 1;

    private static final int COLUMN_DESCRIPTION = 2;

    private static final int COLUMN_LINK = 3;

    public static void setItemData(ItemDialog itemDialog, Cursor cursor) {
        Bundle extras = cursor.getExtras();
        String title = cursor.getString(COLUMN_TITLE);
        String url = cursor.getString(COLUMN_LINK);
        String description = cursor.getString(COLUMN_DESCRIPTION);
        String channel = extras.getString(Channels.TITLE_PLAINTEXT);
        itemDialog.setData(title, url, description, channel);
    }

    public ChannelAdapter(Context context) {
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
