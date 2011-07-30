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
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;
import com.google.android.demos.jamendo.widget.ArtistGalleryAdapter;
import com.google.android.demos.jamendo.widget.CompositeOnItemSelectedListener;
import com.google.android.demos.jamendo.widget.GalleryDecorator;
import com.google.android.demos.jamendo.widget.GalleryScrollListener;
import com.google.android.demos.jamendo.widget.StatusViewManager;
import com.google.android.demos.jamendo.widget.Loadable;

import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
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
import android.widget.Gallery;
import android.widget.TextView;

public class ArtistGalleryActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final int LOADER_ARTISTS = 1;

    private Gallery mGallery;

    private TextView mTextArtistName;

    private TextView mTextArtistGenre;

    private ArtistGalleryAdapter mAdapter;
    
    private Loadable mArtists;

    private int mImageSize;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_gallery);
        mGallery = (Gallery) findViewById(android.R.id.list);
        mTextArtistName = (TextView) findViewById(R.id.text1);
        mTextArtistGenre = (TextView) findViewById(R.id.text2);

        Resources resources = getResources();
        mImageSize = resources.getDimensionPixelSize(R.dimen.gallery_size);

        ArtistDetailsObserver artistDetailsObserver = new ArtistDetailsObserver();
        mAdapter = new ArtistGalleryAdapter(this);
        mAdapter.registerDataSetObserver(artistDetailsObserver);

        mArtists = new Loadable(getSupportLoaderManager(), LOADER_ARTISTS,
                new StatusViewManager(this, LOADER_ARTISTS, this, this));
        
        mGallery.setAdapter(new GalleryDecorator(mAdapter, this));
        mGallery.setOnItemClickListener(this);
        mGallery.setOnItemSelectedListener(new CompositeOnItemSelectedListener(
                artistDetailsObserver, mAdapter, new GalleryScrollListener(mArtists)));
        
        mArtists.init();
    }

    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        Intent intent = getIntent();
        Uri uri = Artists.CONTENT_URI;
        String selectionExtra = intent.getStringExtra(JamendoContract.EXTRA_SELECTION);
        QueryBuilder builder = new QueryBuilder(selectionExtra);
        builder.append(JamendoContract.PARAM_IMAGE_SIZE, String.valueOf(mImageSize));

        String[] projection = ArtistGalleryAdapter.PROJECTION;
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

    /**
     * {@inheritDoc}
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getSelectedItemPosition()) {
            Uri uri = ContentUris.withAppendedId(Artists.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    private void setArtistDetails(Object item) {
        String artistName = mAdapter.getArtistName(item);
        String artistGenre = mAdapter.getArtistGenre(item);
        mTextArtistName.setText(artistName);
        mTextArtistGenre.setText(artistGenre);
    }

    private void clearArtistDetails() {
        mTextArtistName.setText("");
        mTextArtistGenre.setText("");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                mArtists.refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                mArtists.retry();
                break;
        }
    }

    private final class ArtistDetailsObserver extends DataSetObserver implements
            AdapterView.OnItemSelectedListener {
        @Override
        public void onChanged() {
            Object item = mGallery.getSelectedItem();
            setArtistDetails(item);
        }

        /**
         * {@inheritDoc}
         */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getItemAtPosition(position);
            setArtistDetails(item);
        }

        /**
         * {@inheritDoc}
         */
        public void onNothingSelected(AdapterView<?> parent) {
            clearArtistDetails();
        }
    }
}
