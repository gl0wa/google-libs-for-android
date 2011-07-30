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
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.widget.StatusViewManager;
import com.google.android.demos.jamendo.widget.Loadable;
import com.google.android.demos.jamendo.widget.SearchAdapter;

import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

public class SearchActivity extends FragmentActivity implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final int LOADER_SEARCH = 1;

    private SearchAdapter mAdapter;
    
    private Loadable mResults;

    private int mImageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_list);

        Resources resources = getResources();
        mImageSize = resources.getDimensionPixelSize(R.dimen.gallery_size);
        
        mResults = new Loadable(getSupportLoaderManager(), LOADER_SEARCH,
                new StatusViewManager(this, LOADER_SEARCH, this, this));

        mAdapter = new SearchAdapter(this);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_VIEW)) {
            // Forward intents from suggest system to correct activity
            //
            // TODO: Is there a way to force suggest system to
            // not set the component?
            intent.setComponent(null);
            startActivity(intent);
            finish();
        } else {
            mResults.init();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mResults.restart();
    }
    
    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        String query = intent.getStringExtra(SearchManager.QUERY);

        Uri.Builder builder = JamendoContract.AUTHORITY_URI.buildUpon();
        builder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);
        builder.appendPath(query);
        Uri uri = builder.build();
        String[] projection = {
                BaseColumns._ID, SearchManager.SUGGEST_COLUMN_ICON_2,
                SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2
        };
        String selection = JamendoContract.PARAM_IMAGE_SIZE + "=" + mImageSize;
        String[] selectionArgs = null;
        String sortOrder = null;
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
        Uri data = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        startActivity(intent);
    }
    
    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                mResults.retry();
                break;
        }
    }
}
