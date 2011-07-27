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
import com.google.android.demos.jamendo.widget.AdapterStateObserver;
import com.google.android.demos.jamendo.widget.SearchAdapter;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class SearchActivity extends ListActivity {

    private static final int QUERY_SEARCH = 1;

    private SearchAdapter mAdapter;

    private int mImageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_list);

        Resources resources = getResources();
        mImageSize = resources.getDimensionPixelSize(R.dimen.gallery_size);

        mAdapter = new SearchAdapter(this, QUERY_SEARCH);
        setListAdapter(mAdapter);

        AdapterStateObserver observer = new AdapterStateObserver();
        observer.setActivity(this);
        observer.setAdapters(mAdapter);

        if (savedInstanceState == null) {
            changeIntent(getIntent());
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        changeIntent(intent);
    }

    private void changeIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH)) {
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
            String orderBy = null;

            mAdapter.changeQuery(uri, projection, selection, selectionArgs, orderBy);
        } else if (action.equals(Intent.ACTION_VIEW)) {
            // Forward intents from suggest system to correct activity
            //
            // TODO: Is there a way to force suggest system to
            // not set the component?
            intent.setComponent(null);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri data = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        startActivity(intent);
    }
}
