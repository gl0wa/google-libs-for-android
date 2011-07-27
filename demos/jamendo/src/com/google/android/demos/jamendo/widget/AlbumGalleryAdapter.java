/*-
 * Copyright (C) 2010 Google Inc.
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

package com.google.android.demos.jamendo.widget;

import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumGalleryAdapter extends JamendoGalleryAdapter implements
        AdapterView.OnItemSelectedListener {

    public static final String[] PROJECTION = {
            Albums._ID, Albums.IMAGE, Artists.NAME, Albums.NAME, Albums.GENRE
    };

    private static final int COLUMN_ALBUM_IMAGE = 1;

    private static final int COLUMN_ARTIST_NAME = 2;

    private static final int COLUMN_ALBUM_NAME = 3;

    private static final int COLUMN_ALBUM_GENRE = 4;

    private final TextView mTextAlbumName;

    private final TextView mTextArtistName;

    private final TextView mTextAlbumGenre;

    public AlbumGalleryAdapter(Activity context, int queryId) {
        super(context, queryId);
        mTextAlbumName = (TextView) context.findViewById(R.id.text1);
        mTextArtistName = (TextView) context.findViewById(R.id.text2);
        mTextAlbumGenre = (TextView) context.findViewById(R.id.text3);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        String url = cursor.getString(COLUMN_ALBUM_IMAGE);
        mImageLoader.bind(this, imageView, url);
    }

    /**
     * {@inheritDoc}
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setAlbumDetails(position);
        mImageLoader.preload(getCursor(), COLUMN_ALBUM_IMAGE, position - 5, position + 5);
    }

    /**
     * {@inheritDoc}
     */
    public void onNothingSelected(AdapterView<?> parent) {
        clearAlbumDetails();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        int position = mGallery.getSelectedItemPosition();
        setAlbumDetails(position);

        // Pre-fetch album covers
        Cursor cursor = getCursor();
        mImageLoader.prefetch(cursor, COLUMN_ALBUM_IMAGE);
    }

    private void setAlbumDetails(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            String albumName = cursor.getString(COLUMN_ALBUM_NAME);
            String artistName = cursor.getString(COLUMN_ARTIST_NAME);
            String albumGenre = cursor.getString(COLUMN_ALBUM_GENRE);
            mTextAlbumName.setText(albumName);
            mTextArtistName.setText(artistName);
            mTextAlbumGenre.setText(albumGenre);
        } else {
            clearAlbumDetails();
        }
    }

    private void clearAlbumDetails() {
        mTextAlbumName.setText("");
        mTextArtistName.setText("");
        mTextAlbumGenre.setText("");
    }
}
