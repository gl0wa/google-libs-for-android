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
import com.google.android.feeds.widget.BaseFeedAdapter;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class TagActivity extends JamendoActivity {

    @Override
    protected BaseFeedAdapter createHeaderAdapter() {
        String[] from = {
                Tags._ID, Tags.NAME
        };
        int[] to = {
                android.R.id.icon, android.R.id.text1
        };
        return new SimpleFeedAdapter(this, QUERY_HEADER, R.layout.jamendo_tag_header, from, to);
    }

    @Override
    protected ListAdapter createSeparatorAdapter() {
        return new ListSeparatorAdapter(R.string.list_title_albums);
    }

    @Override
    protected BaseFeedAdapter createListAdapter() {
        String[] from = {
                Albums.IMAGE, Artists.NAME, Albums.NAME, Albums.GENRE
        };
        int[] to = {
                R.id.icon, R.id.text1, R.id.text2, R.id.text3
        };
        return new SimpleFeedAdapter(this, QUERY_LIST, R.layout.jamendo_list_item_3, from, to);
    }

    @Override
    protected void changeIntent(Intent intent) {
        changeHeaderAdapterQuery(intent);
        changeListAdapterQuery(intent);
    }

    private void changeHeaderAdapterQuery(Intent intent) {
        Uri uri = intent.getData();
        String[] projection = {
                Tags._ID, Tags.IDSTR, Tags.NAME
        };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        mHeaderAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    private void changeListAdapterQuery(Intent intent) {
        String idstr = intent.getData().getLastPathSegment();
        Uri uri = Albums.CONTENT_URI;
        String[] projection = {
                Albums._ID, Albums.IMAGE, Artists.NAME, Albums.NAME, Albums.GENRE
        };
        String selection = String.format("%s=?", Tags.IDSTR);
        String[] selectionArgs = {
            idstr
        };
        String sortOrder = Albums.Order.RATINGMONTH.descending();
        mListAdapter.changeQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
