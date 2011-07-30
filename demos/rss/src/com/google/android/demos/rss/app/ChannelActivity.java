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

package com.google.android.demos.rss.app;

import com.google.android.demos.rss.R;
import com.google.android.demos.rss.provider.RssContract.Channels;
import com.google.android.demos.rss.provider.RssContract.Items;
import com.google.android.demos.rss.widget.ChannelAdapter;
import com.google.android.demos.rss.widget.ItemDialog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChannelActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SimpleCursorAdapter.ViewBinder,
        AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String DEFAULT_CHANNEL = "http://feeds.digg.com/digg/popular.rss";

    private static final int MENU_GROUP_INTENT_OPTIONS = 1;

    private static final int DIALOG_ITEM = 1;

    private static final int LOADER_CHANNEL = 1;

    private ChannelAdapter mAdapter;

    private ItemDialog mItemDialog;
    
    private View mEmpty;

    private View mLoading;

    private View mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.list_activity);
        ListView listView = (ListView) findViewById(android.R.id.list);
        mEmpty = findViewById(R.id.empty);
        mLoading = findViewById(R.id.loading);
        mError = findViewById(R.id.error);
        mError.findViewById(R.id.retry).setOnClickListener(this);

        // Loader from last Activity instance may still be loading
        mLoading.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);

        Context context = this;
        int layout = android.R.layout.simple_list_item_1;
        String[] from = {
            Items.TITLE
        };
        int[] to = {
            android.R.id.text1
        };
        SimpleCursorAdapter innerAdapter = new SimpleCursorAdapter(context, layout, null, from, to);
        innerAdapter.setViewBinder(this);

        mAdapter = new ChannelAdapter(this);
        mAdapter.registerDataSetObserver(new TitleObserver());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        getSupportLoaderManager().initLoader(LOADER_CHANNEL, Bundle.EMPTY, this);
    }
    
    /**
     * {@inheritDoc}
     */
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mLoading.setVisibility(mAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        setWindowIndeterminateProgressVisible(!mAdapter.isEmpty());
        Context context = this;
        Uri data = getIntent().getData();
        Uri uri = data != null ? data : Items.contentUri(DEFAULT_CHANNEL);
        return ChannelAdapter.createLoader(context, uri);
    }
    
    /**
     * {@inheritDoc}
     */
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(mAdapter.isEmpty() && !mAdapter.hasError() ? View.VISIBLE : View.GONE);
        mError.setVisibility(mAdapter.isEmpty() && mAdapter.hasError() ? View.VISIBLE : View.GONE);
        setWindowIndeterminateProgressVisible(false);
    }

    /**
     * {@inheritDoc}
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        setWindowIndeterminateProgressVisible(false);
    }
    
    private void setWindowIndeterminateProgressVisible(boolean value) {
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                value ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ITEM) {
            mItemDialog = new ItemDialog(this);
            return mItemDialog;
        } else {
            return super.onCreateDialog(id);
        }
    }
    
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Object item = l.getItemAtPosition(position);
        Cursor cursor = mAdapter.getCursor();
        if (item != null && item == cursor) {
            showDialog(DIALOG_ITEM);

            // Assign item data to dialog from cursor
            ChannelAdapter.setItemData(mItemDialog, cursor);

            // Scroll to top of item content
            mItemDialog.scrollToTop();
        }
    }

    private void addStandardOptionsMenuItems(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.channel_menu, menu);
    }

    private void addAlternativeOptionsMenuItems(Menu menu) {
        int groupId = MENU_GROUP_INTENT_OPTIONS;
        int itemId = Menu.NONE;
        int order = Menu.NONE;
        ComponentName caller = getComponentName();
        Intent[] specifics = null;
        Intent intent = new Intent();
        intent.setData(getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        int flags = 0;
        MenuItem[] outSpecificItems = null;
        menu.addIntentOptions(groupId, itemId, order, caller, specifics, intent, flags,
                outSpecificItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        addStandardOptionsMenuItems(menu);
        addAlternativeOptionsMenuItems(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                reload();
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (android.R.id.text1 == view.getId()) {
            TextView textView = (TextView) view;
            String value = cursor.getString(columnIndex);
            CharSequence text = Html.fromHtml(value);
            textView.setText(text);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                reload();
                break;
        }
    }

    private void reload() {
        getSupportLoaderManager().restartLoader(LOADER_CHANNEL, Bundle.EMPTY, this);
    }

    private final class TitleObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            Cursor cursor = mAdapter.getCursor();
            Bundle extras = cursor.getExtras();
            String title = extras.getString(Channels.TITLE_PLAINTEXT);
            if (title != null) {
                setTitle(title);
            }
        }
    }
}
