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
import com.google.android.demos.jamendo.provider.JamendoContract.Playlists;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

public class PlaylistHeaderAdapter extends TrackListHeaderAdapter {
    private static final String[] FROM = new String[] {
            Users.IMAGE, Playlists.NAME, Users.NAME, Playlists.ID, Playlists.ID
    };

    private static final int[] TO = new int[] {
            R.id.icon, R.id.text1, R.id.text2, R.id.link1, R.id.link2
    };

    public PlaylistHeaderAdapter(Activity context, int queryId) {
        super(context, queryId, R.layout.jamendo_header, FROM, TO);
        setDefaultImageUrl(JamendoApp.DEFAULT_USER_AVATAR_100);
    }

    @Override
    protected boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.text2:
                TextView textView = (TextView) view;
                String userName = cursor.getString(cursor.getColumnIndexOrThrow(Users.NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(Users.ID));
                Uri uri = ContentUris.withAppendedId(Users.CONTENT_URI, id);
                JamendoApp.setTextToLink(textView, userName, uri);
                return true;
            default:
                return super.setViewValue(view, cursor, columnIndex);
        }
    }
}
