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

import com.google.android.demos.rss.R;
import com.google.android.demos.rss.provider.RssContract.Channels;
import com.google.android.demos.rss.provider.RssContract.Items;
import com.google.android.feeds.widget.AdapterState;
import com.google.android.feeds.widget.BaseFeedAdapter;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Adapter for an RSS channel.
 */
public class ChannelAdapter extends BaseFeedAdapter {

    private static final String[] PROJECTION = {
            Items._ID, Items.TITLE_PLAINTEXT, Items.DESCRIPTION, Items.LINK
    };

    private static final int COLUMN_TITLE = 1;

    private static final int COLUMN_DESCRIPTION = 2;

    private static final int COLUMN_LINK = 3;

    private static void setViewVisible(View view, boolean visible) {
        if (view != null) {
            int visibility = visible ? View.VISIBLE : View.GONE;
            if (visibility != view.getVisibility()) {
                view.setVisibility(visibility);
            }
        }
    }

    private static void setWindowIndeterminateProgressVisible(Window window, boolean visible) {
        int featureId = Window.FEATURE_INDETERMINATE_PROGRESS;
        int value = visible ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF;
        window.setFeatureInt(featureId, value);
    }

    public static void setItemData(ItemDialog itemDialog, Cursor cursor) {
        Bundle extras = cursor.getExtras();
        String title = cursor.getString(COLUMN_TITLE);
        String url = cursor.getString(COLUMN_LINK);
        String description = cursor.getString(COLUMN_DESCRIPTION);
        String channel = extras.getString(Channels.TITLE_PLAINTEXT);
        itemDialog.setData(title, url, description, channel);
    }
    
    private final AdapterState mAdapterState;

    private final View mEmpty;

    private final View mLoading;

    private final View mError;

    private final Window mWindow;

    public ChannelAdapter(ListActivity activity, int queryId) {
        super(activity, queryId);
        ListView listView = activity.getListView();
        mAdapterState = new AdapterState();
        mAdapterState.setAdapter(this);
        mAdapterState.setAdapterView(listView);
        mEmpty = activity.findViewById(android.R.id.empty);
        mLoading = activity.findViewById(R.id.loading);
        mError = activity.findViewById(R.id.error);
        mWindow = activity.getWindow();
        mError.findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            /** {@inheritDoc} */
            public void onClick(View v) {
                retry();
            }
        });
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapterState.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mAdapterState.onRestoreInstanceState(state);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        Cursor cursor = getCursor();
        Bundle extras = cursor.getExtras();
        String title = extras.getString(Channels.TITLE_PLAINTEXT);
        if (title != null) {
            mWindow.setTitle(title);
        }
    }

    @Override
    protected void onQueryStateChanged() {
        super.onQueryStateChanged();
        setViewVisible(mEmpty, false);
        setViewVisible(mLoading, false);
        setViewVisible(mError, false);
        setWindowIndeterminateProgressVisible(mWindow, false);
        boolean isLoaded = hasCursor() && isQueryDone();
        if (isLoaded) {
            if (isEmpty()) {
                if (hasError()) {
                    setViewVisible(mError, true);
                } else {
                    setViewVisible(mEmpty, true);
                }
            }
        } else {
            if (isEmpty()) {
                setViewVisible(mLoading, true);
            } else {
                setWindowIndeterminateProgressVisible(mWindow, true);
            }
        }
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

    public void changeChannelUrl(String url) {
        if (url != null) {
            Uri uri = Items.contentUri(url);
            changeQuery(uri, PROJECTION, null, null, null);
        } else {
            clear();
        }
    }
}
