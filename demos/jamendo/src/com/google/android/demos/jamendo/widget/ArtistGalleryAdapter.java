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
import com.google.android.imageloader.ImageLoader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

public class ArtistGalleryAdapter extends CursorAdapter implements
        AdapterView.OnItemSelectedListener {

    public static final String[] PROJECTION = {
            Artists._ID, Artists.IMAGE, Artists.NAME, Artists.GENRE
    };

    private static final int COLUMN_ARTIST_IMAGE = 1;

    private static final int COLUMN_ARTIST_NAME = 2;

    private static final int COLUMN_ARTIST_GENRE = 3;
    
    private ImageLoader mImageLoader;

    public ArtistGalleryAdapter(Context context) {
        super(context, null, 0);
        mImageLoader = ImageLoader.get(context);
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_gallery_item, parent, false);
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

    private String getString(Object item, int columnIndex) {
        Cursor cursor = getCursor();
        if (item == cursor) {
            return cursor.getString(columnIndex);
        } else {
            return null;
        }
    }

    public String getArtistName(Object item) {
        return getString(item, COLUMN_ARTIST_NAME);
    }

    public String getArtistGenre(Object item) {
        return getString(item, COLUMN_ARTIST_GENRE);
    }

    /**
     * {@inheritDoc}
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mImageLoader.preload(getCursor(), COLUMN_ARTIST_IMAGE, position - 5, position + 5);
    }

    /**
     * {@inheritDoc}
     */
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
