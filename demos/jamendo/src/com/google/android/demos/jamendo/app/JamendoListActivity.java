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

package com.google.android.demos.jamendo.app;

import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.net.QueryBuilder;
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.widget.AdapterStateObserver;
import com.google.android.demos.jamendo.widget.SimpleFeedAdapter;
import com.google.android.feeds.widget.AdapterState;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

abstract class JamendoListActivity extends ListActivity {

    private static final int QUERY_LIST = 1;

    protected SimpleFeedAdapter mAdapter;

    private AdapterState mListState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_list);

        mAdapter = new SimpleFeedAdapter(this, QUERY_LIST, getLayout(), getFrom(), getTo());
        setListAdapter(mAdapter);
        
        AdapterStateObserver observer = new AdapterStateObserver();
        observer.setActivity(this);
        observer.setAdapters(mAdapter);
        
        mListState = new AdapterState();
        mListState.setAdapterView(getListView());
        mListState.setAdapter(getListAdapter());

        if (savedInstanceState == null) {
            changeIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.onSaveInstanceState(outState);
        mListState.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mAdapter.onRestoreInstanceState(state);
        mListState.onRestoreInstanceState(state);
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

    protected abstract String[] getProjection();

    protected abstract int getLayout();

    protected abstract String[] getFrom();

    protected abstract int[] getTo();

    protected int getImageSize() {
        return R.dimen.thumbnail_size;
    }

    protected final String getDimensionPixelSizeAsString(int resId) {
        Resources resources = getResources();
        int size = resources.getDimensionPixelSize(resId);
        return String.valueOf(size);
    }

    private void changeIntent(Intent intent) {
        Uri uri = intent.getData();

        String selectionExtra = intent.getStringExtra(JamendoContract.EXTRA_SELECTION);
        QueryBuilder builder = new QueryBuilder(selectionExtra);
        builder.append(JamendoContract.PARAM_IMAGE_SIZE, getDimensionPixelSizeAsString(getImageSize()));

        String[] projection = getProjection();
        String selection = builder.build();
        String[] selectionArgs = intent.getStringArrayExtra(JamendoContract.EXTRA_SELECTION_ARGS);
        String sortOrder = intent.getStringExtra(JamendoContract.EXTRA_SORT_ORDER);

        mAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Uri contentUri = mAdapter.getUri();
        Uri uri = ContentUris.withAppendedId(contentUri, id);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                mAdapter.refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
