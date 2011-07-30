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
import com.google.android.demos.jamendo.widget.CompositeListAdapter;
import com.google.android.demos.jamendo.widget.StatusViewManager;
import com.google.android.demos.jamendo.widget.Loadable;
import com.google.android.imageloader.ImageLoader;

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class JamendoActivity extends FragmentActivity implements
        AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {

    protected static final int LOADER_HEADER = 1;

    protected static final int LOADER_LIST = 2;

    protected ImageLoader mImageLoader;
    
    protected Loadable mHeader;
    
    protected Loadable mList;

    protected CursorAdapter mHeaderAdapter;

    protected ListAdapter mSeparatorAdapter;

    protected CursorAdapter mListAdapter;

    protected abstract CursorAdapter createHeaderAdapter();

    protected abstract ListAdapter createSeparatorAdapter();

    protected abstract CursorAdapter createListAdapter();

    protected final String getDimensionPixelSizeAsString(int resId) {
        Resources resources = getResources();
        int size = resources.getDimensionPixelSize(resId);
        return Integer.toString(size);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_list);

        mImageLoader = ImageLoader.get(this);
        mHeaderAdapter = createHeaderAdapter();
        mSeparatorAdapter = createSeparatorAdapter();
        mListAdapter = createListAdapter();
        
        LoaderManager.LoaderCallbacks<Cursor> callbacks = new StatusViewManager(this, LOADER_LIST, this, this);
        mHeader = new Loadable(getSupportLoaderManager(), LOADER_HEADER, callbacks);
        mList = new Loadable(getSupportLoaderManager(), LOADER_LIST, callbacks);
        
        ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(new CompositeListAdapter(mHeaderAdapter, mSeparatorAdapter, mListAdapter));
        list.setOnItemClickListener(this);

        // Load up to 100 list items initially instead of loading incrementally
        mHeader.init();
        mList.init(100);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_HEADER:
                mHeaderAdapter.swapCursor(data);
                break;
            case LOADER_LIST:
                mListAdapter.swapCursor(data);
                break;
        }
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_HEADER:
                mHeaderAdapter.swapCursor(null);
                break;
            case LOADER_LIST:
                mListAdapter.swapCursor(null);
                break;
        }
    }
    
    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                mHeader.retry();
                mList.retry();
                break;
        }
    }

    public final void refresh() {
        mHeader.refresh();
        mList.refresh();
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
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
