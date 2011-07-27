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
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;
import com.google.android.demos.jamendo.widget.ListSeparatorAdapter;
import com.google.android.demos.jamendo.widget.SimpleFeedAdapter;
import com.google.android.feeds.widget.BaseFeedAdapter;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ArtistActivity extends JamendoActivity {

    private static final Uri BASE_URI = Uri.parse("http://www.jamendo.com/artist");

    @Override
    protected BaseFeedAdapter createHeaderAdapter() {
        String[] from = {
                Artists.IMAGE, Artists.NAME
        };
        int[] to = {
                R.id.icon, R.id.text1
        };
        SimpleFeedAdapter adapter = new SimpleFeedAdapter(this, QUERY_HEADER,
                R.layout.jamendo_header, from, to);
        adapter.setDefaultImageUrl(JamendoApp.DEFAULT_ARTIST_AVATAR);
        return adapter;
    }

    @Override
    protected ListAdapter createSeparatorAdapter() {
        return new ListSeparatorAdapter(R.string.list_title_albums);
    }

    @Override
    protected void changeIntent(Intent intent) {
        changeHeaderAdapterQuery(intent);
        changeListAdapterQuery(intent);
    }

    private void changeHeaderAdapterQuery(Intent intent) {
        Uri uri = intent.getData();
        String[] projection = {
                Artists._ID, Artists.IDSTR, Artists.IMAGE, Artists.NAME
        };
        String selection = String.format("%s=?", JamendoContract.PARAM_IMAGE_SIZE);
        String[] selectionArgs = {
            getDimensionPixelSizeAsString(R.dimen.image_size)
        };
        String sortOrder = null;
        mHeaderAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    private void changeListAdapterQuery(Intent intent) {
        Uri uri = Albums.CONTENT_URI;
        String[] projection = {
                Albums._ID, Albums.IMAGE, Albums.NAME
        };
        Uri data = intent.getData();
        Long artistId = null;
        String artistIdStr = null;
        try {
            artistId = Long.valueOf(ContentUris.parseId(data));
        } catch (NumberFormatException e) {
            artistIdStr = data.getLastPathSegment();
        }
        String selection = String.format("%s=?&%s=?",
                artistId != null ? Artists.ID : Artists.IDSTR, JamendoContract.PARAM_IMAGE_SIZE);
        String[] selectionArgs = {
                artistId != null ? artistId.toString() : artistIdStr,
                getDimensionPixelSizeAsString(R.dimen.image_size)
        };
        String sortOrder = Albums.Order.RELEASEDATE.descending();

        mListAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    protected SimpleFeedAdapter createListAdapter() {
        String[] from = {
                Albums.IMAGE, Albums.NAME
        };
        int[] to = {
                R.id.icon, R.id.text1
        };
        return new SimpleFeedAdapter(this, QUERY_LIST, R.layout.jamendo_list_item_1, from, to);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.artist_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                share();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void share() {
        Cursor cursor = mHeaderAdapter.getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            String idstr = cursor.getString(cursor.getColumnIndexOrThrow(Artists.IDSTR));
            String artistName = cursor.getString(cursor.getColumnIndexOrThrow(Artists.NAME));
            Uri uri = BASE_URI.buildUpon().appendPath(idstr).build();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, artistName);
            intent.putExtra(Intent.EXTRA_TEXT, String.valueOf(uri));
            intent = Intent.createChooser(intent, null);
            startActivity(intent);
        }
    }
}
