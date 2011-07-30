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

public class AlbumGalleryActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final int LOADER_ALBUMS = 1;

    private Gallery mGallery;

    private TextView mTextAlbumName;

    private TextView mTextArtistName;

    private TextView mTextAlbumGenre;

    private AlbumGalleryAdapter mAdapter;
    
    private Loadable mAlbums;

    private int mImageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.jamendo_gallery);
        mGallery = (Gallery) findViewById(android.R.id.list);
        mTextAlbumName = (TextView) findViewById(R.id.text1);
        mTextArtistName = (TextView) findViewById(R.id.text2);
        mTextAlbumGenre = (TextView) findViewById(R.id.text3);

        Resources resources = getResources();
        mImageSize = resources.getDimensionPixelSize(R.dimen.gallery_size);

        AlbumDetailsObserver albumDetailsObserver = new AlbumDetailsObserver();
        mAdapter = new AlbumGalleryAdapter(this);
        mAdapter.registerDataSetObserver(albumDetailsObserver);
        mAlbums = new Loadable(getSupportLoaderManager(), LOADER_ALBUMS,
                new StatusViewManager(this, LOADER_ALBUMS, this, this));
        mGallery.setAdapter(new GalleryDecorator(mAdapter, this));
        mGallery.setOnItemClickListener(this);
        mGallery.setOnItemSelectedListener(new CompositeOnItemSelectedListener(
                albumDetailsObserver, mAdapter, new GalleryScrollListener(mAlbums)));
        mAlbums.init();
    }
        
    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Uri uri = Albums.CONTENT_URI;
        String selectionExtra = intent.getStringExtra(JamendoContract.EXTRA_SELECTION);
        QueryBuilder builder = new QueryBuilder(selectionExtra);
        builder.append(JamendoContract.PARAM_IMAGE_SIZE, String.valueOf(mImageSize));

        String[] projection = AlbumGalleryAdapter.PROJECTION;
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
            Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
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
                mAlbums.refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                mAlbums.retry();
                break;
        }
    }

    private void setAlbumDetails(Object item) {
        String albumName = mAdapter.getAlbumName(item);
        String artistName = mAdapter.getArtistName(item);
        String albumGenre = mAdapter.getAlbumGenre(item);
        mTextAlbumName.setText(albumName);
        mTextArtistName.setText(artistName);
        mTextAlbumGenre.setText(albumGenre);
    }

    private void clearAlbumDetails() {
        mTextAlbumName.setText("");
        mTextArtistName.setText("");
        mTextAlbumGenre.setText("");
    }

    private final class AlbumDetailsObserver extends DataSetObserver implements
            AdapterView.OnItemSelectedListener {
        @Override
        public void onChanged() {
            Object item = mGallery.getSelectedItem();
            setAlbumDetails(item);
        }

        /**
         * {@inheritDoc}
         */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getItemAtPosition(position);
            setAlbumDetails(item);
        }

        /**
         * {@inheritDoc}
         */
        public void onNothingSelected(AdapterView<?> parent) {
            clearAlbumDetails();
        }
    }
}
