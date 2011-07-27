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
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

public class AlbumHeaderAdapter extends TrackListHeaderAdapter {
    private static final String[] FROM = {
            Albums.IMAGE, Albums.NAME, Artists.NAME, Albums.GENRE, Albums.ID, Albums.ID
    };

    private static final int[] TO = {
            R.id.icon, R.id.text1, R.id.text2, R.id.text3, R.id.link1, R.id.link2
    };

    public AlbumHeaderAdapter(Activity context, int queryId) {
        super(context, queryId, R.layout.jamendo_header, FROM, TO);
    }

    @Override
    protected boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.text2:
                TextView textView = (TextView) view;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(Artists.NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(Artists.ID));
                Uri uri = ContentUris.withAppendedId(Artists.CONTENT_URI, id);
                JamendoApp.setTextToLink(textView, name, uri);
                return true;
            default:
                return super.setViewValue(view, cursor, columnIndex);
        }
    }
}
