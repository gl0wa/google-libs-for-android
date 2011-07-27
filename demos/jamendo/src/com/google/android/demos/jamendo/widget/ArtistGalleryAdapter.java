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
import com.google.android.demos.jamendo.app.JamendoApp;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

public class ArtistGalleryAdapter extends JamendoGalleryAdapter implements
        AdapterView.OnItemSelectedListener {

    public static final String[] PROJECTION = {
            Artists._ID, Artists.IMAGE, Artists.NAME, Artists.GENRE
    };

    private static final int COLUMN_ARTIST_IMAGE = 1;

    private static final int COLUMN_ARTIST_NAME = 2;

    private static final int COLUMN_ARTIST_GENRE = 3;

    private final TextView mTextArtistName;

    private final TextView mTextArtistGenre;

    public ArtistGalleryAdapter(Activity context, int queryId) {
        super(context, queryId);
        mTextArtistName = (TextView) context.findViewById(R.id.text1);
        mTextArtistGenre = (TextView) context.findViewById(R.id.text2);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        String url = cursor.getString(COLUMN_ARTIST_IMAGE);
        if (url.length() == 0) {
            url = JamendoApp.DEFAULT_ARTIST_AVATAR;
        }
        mImageLoader.bind(this, imageView, url);
    }

    /**
     * {@inheritDoc}
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setArtistDetails(position);
        mImageLoader.preload(getCursor(), COLUMN_ARTIST_IMAGE, position - 5, position + 5);
    }

    /**
     * {@inheritDoc}
     */
    public void onNothingSelected(AdapterView<?> parent) {
        clearArtistDetails();
    }

    private void setArtistDetails(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            String artistName = cursor.getString(COLUMN_ARTIST_NAME);
            String artistGenre = cursor.getString(COLUMN_ARTIST_GENRE);
            mTextArtistName.setText(artistName);
            mTextArtistGenre.setText(artistGenre);
        } else {
            clearArtistDetails();
        }
    }

    private void clearArtistDetails() {
        mTextArtistName.setText("");
        mTextArtistGenre.setText("");
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        int position = mGallery.getSelectedItemPosition();
        setArtistDetails(position);
    }
}
