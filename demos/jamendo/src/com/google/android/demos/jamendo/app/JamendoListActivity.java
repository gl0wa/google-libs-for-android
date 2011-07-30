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
import com.google.android.demos.jamendo.widget.ListDecorator;
import com.google.android.demos.jamendo.widget.StatusViewManager;
import com.google.android.demos.jamendo.widget.ListScrollListener;
import com.google.android.demos.jamendo.widget.Loadable;
import com.google.android.demos.jamendo.widget.SimpleFeedAdapter;

import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

abstract class JamendoListActivity extends FragmentActivity implements
        AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {
    

    private static final int LOADER_LIST = 1;

    protected SimpleFeedAdapter mAdapter;
    
    protected Loadable mItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_list);

        mAdapter = new SimpleFeedAdapter(this, getLayout(), getFrom(), getTo());
        mItems = new Loadable(getSupportLoaderManager(), LOADER_LIST, new StatusViewManager(
                this, LOADER_LIST, this, this));

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(new ListDecorator(mAdapter, this));
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(new ListScrollListener(mItems));
        
        mItems.init();
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

    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        String selectionExtra = intent.getStringExtra(JamendoContract.EXTRA_SELECTION);
        QueryBuilder builder = new QueryBuilder(selectionExtra);
        builder.append(JamendoContract.PARAM_IMAGE_SIZE, getDimensionPixelSizeAsString(getImageSize()));

        String[] projection = getProjection();
        String selection = builder.build();
        String[] selectionArgs = intent.getStringArrayExtra(JamendoContract.EXTRA_SELECTION_ARGS);
        String sortOrder = intent.getStringExtra(JamendoContract.EXTRA_SORT_ORDER);
        return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Uri contentUri = getIntent().getData();
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
                mItems.refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                mItems.retry();
                break;
        }
    }
}
