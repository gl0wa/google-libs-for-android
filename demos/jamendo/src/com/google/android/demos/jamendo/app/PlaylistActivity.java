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
import com.google.android.demos.jamendo.provider.JamendoContract.Playlists;
import com.google.android.demos.jamendo.provider.JamendoContract.Tracks;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;
import com.google.android.demos.jamendo.widget.ListSeparatorAdapter;
import com.google.android.demos.jamendo.widget.PlaylistHeaderAdapter;
import com.google.android.demos.jamendo.widget.TrackListAdapter;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class PlaylistActivity extends JamendoActivity {

    private static final int MENU_GROUP_INTENT_OPTIONS = 1;

    @Override
    protected CursorAdapter createHeaderAdapter() {
        return new PlaylistHeaderAdapter(this);
    }

    @Override
    protected ListAdapter createSeparatorAdapter() {
        return new ListSeparatorAdapter(R.string.list_title_tracks);
    }

    @Override
    protected CursorAdapter createListAdapter() {
        return new TrackListAdapter(this);
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        TrackListAdapter.playTrack(this, id);
    }

    /** {@inheritDoc} */
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case LOADER_HEADER: {
                Uri uri = getIntent().getData();
                String[] projection = {
                        Playlists._ID, Users.IMAGE, Playlists.NAME, Users.NAME, Users.ID, Playlists.ID
                };
                String selection = String.format("%s=?", JamendoContract.PARAM_IMAGE_SIZE);
                String[] selectionArgs = {
                    getDimensionPixelSizeAsString(R.dimen.image_size)
                };
                String sortOrder = null;
                return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);
            }
            case LOADER_LIST: {
                Uri uri = Tracks.CONTENT_URI;
                String[] projection = TrackListAdapter.PROJECTION;
                String selection = String.format("%s=?&%s=?&%s=?", JamendoContract.PARAM_JOIN,
                        JamendoContract.PARAM_JOIN, Playlists.ID);
                Uri data = getIntent().getData();
                long playlistId = ContentUris.parseId(data);
                String[] selectionArgs = {
                        JamendoContract.JOIN_TRACK_ALBUM, JamendoContract.JOIN_ALBUM_ARTIST,
                        String.valueOf(playlistId)
                };
                String sortOrder = Tracks.Order.NUMPLAYLIST.ascending();
                return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);
            }
            default:
                return null;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        int groupId = MENU_GROUP_INTENT_OPTIONS;
        int itemId = Menu.NONE;
        int order = Menu.NONE;
        ComponentName caller = getComponentName();
        Intent[] specifics = null;
        Intent intent = new Intent();
        long id = ContentUris.parseId(getIntent().getData());
        intent.setDataAndType(
                JamendoContract.createPlaylistUri(JamendoContract.FORMAT_M3U, Playlists.ID, id),
                JamendoContract.CONTENT_TYPE_M3U);
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        int flags = 0;
        MenuItem[] outSpecificItems = null;
        menu.addIntentOptions(groupId, itemId, order, caller, specifics, intent, flags,
                outSpecificItems);
        return menu.hasVisibleItems();
    }
}
