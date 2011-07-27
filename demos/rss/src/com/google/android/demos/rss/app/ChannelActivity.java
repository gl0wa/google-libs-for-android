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
import com.google.android.demos.rss.provider.RssContract.Items;
import com.google.android.demos.rss.widget.ChannelAdapter;
import com.google.android.demos.rss.widget.ItemDialog;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChannelActivity extends ListActivity implements SimpleCursorAdapter.ViewBinder {

    private static final int MENU_GROUP_INTENT_OPTIONS = 1;

    private static final int DIALOG_ITEM = 1;

    private static final int QUERY_CHANNEL = 1;

    private ChannelAdapter mAdapter;

    private ItemDialog mItemDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.list_activity);

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

        mAdapter = new ChannelAdapter(this, QUERY_CHANNEL);
        setListAdapter(mAdapter);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            Uri uri = intent.getData();
            String channelUrl = uri != null ? Items.getChannelUrl(uri)
                    : "http://feeds.digg.com/digg/popular.rss";
            mAdapter.changeChannelUrl(channelUrl);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mAdapter.onRestoreInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.onStop();
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
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
                mAdapter.refresh();
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
}
