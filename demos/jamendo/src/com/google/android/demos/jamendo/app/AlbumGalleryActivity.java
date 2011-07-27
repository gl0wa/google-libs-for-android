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
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.widget.AlbumGalleryAdapter;
import com.google.android.demos.jamendo.widget.CompositeOnItemSelectedListener;
import com.google.android.demos.jamendo.widget.GalleryScrollListener;
import com.google.android.demos.jamendo.widget.AdapterStateObserver;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;

public class AlbumGalleryActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final int QUERY_ALBUMS = 1;

    private Gallery mGallery;

    private AlbumGalleryAdapter mAdapter;

    private int mImageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.jamendo_gallery);
        mGallery = (Gallery) findViewById(android.R.id.list);

        Resources resources = getResources();
        mImageSize = resources.getDimensionPixelSize(R.dimen.gallery_size);

        mAdapter = new AlbumGalleryAdapter(this, QUERY_ALBUMS);
        mGallery.setAdapter(mAdapter);
        mGallery.setOnItemClickListener(this);
        mGallery.setOnItemSelectedListener(new CompositeOnItemSelectedListener(mAdapter, new GalleryScrollListener(mAdapter)));
        
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

    private void changeIntent(Intent intent) {
        Uri uri = Albums.CONTENT_URI;
        String selectionExtra = intent.getStringExtra(JamendoContract.EXTRA_SELECTION);
        QueryBuilder builder = new QueryBuilder(selectionExtra);
        builder.append(JamendoContract.PARAM_IMAGE_SIZE, String.valueOf(mImageSize));

        String[] projection = AlbumGalleryAdapter.PROJECTION;
        String selection = builder.build();
        String[] selectionArgs = intent.getStringArrayExtra(JamendoContract.EXTRA_SELECTION_ARGS);
        String sortOrder = intent.getStringExtra(JamendoContract.EXTRA_SORT_ORDER);

        mAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getSelectedItemPosition()) {
            Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
}
