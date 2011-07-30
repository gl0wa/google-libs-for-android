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
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;
import com.google.android.demos.jamendo.provider.JamendoContract.Tags;
import com.google.android.demos.jamendo.widget.ListSeparatorAdapter;
import com.google.android.demos.jamendo.widget.SimpleFeedAdapter;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class TagActivity extends JamendoActivity {

    @Override
    protected CursorAdapter createHeaderAdapter() {
        String[] from = {
                Tags._ID, Tags.NAME
        };
        int[] to = {
                android.R.id.icon, android.R.id.text1
        };
        return new SimpleFeedAdapter(this, R.layout.jamendo_tag_header, from, to);
    }

    @Override
    protected ListAdapter createSeparatorAdapter() {
        return new ListSeparatorAdapter(R.string.list_title_albums);
    }

    @Override
    protected CursorAdapter createListAdapter() {
        String[] from = {
                Albums.IMAGE, Artists.NAME, Albums.NAME, Albums.GENRE
        };
        int[] to = {
                R.id.icon, R.id.text1, R.id.text2, R.id.text3
        };
        return new SimpleFeedAdapter(this, R.layout.jamendo_list_item_3, from, to);
    }
    
    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case LOADER_HEADER: {
                Uri uri = getIntent().getData();
                String[] projection = {
                        Tags._ID, Tags.IDSTR, Tags.NAME
                };
                String selection = null;
                String[] selectionArgs = null;
                String sortOrder = null;
                return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);
            }
            case LOADER_LIST: {
                String idstr = getIntent().getData().getLastPathSegment();
                Uri uri = Albums.CONTENT_URI;
                String[] projection = {
                        Albums._ID, Albums.IMAGE, Artists.NAME, Albums.NAME, Albums.GENRE
                };
                String selection = String.format("%s=?", Tags.IDSTR);
                String[] selectionArgs = {
                    idstr
                };
                String sortOrder = Albums.Order.RATINGMONTH.descending();
                return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);
            }
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
