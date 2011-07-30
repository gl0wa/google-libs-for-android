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
import com.google.android.demos.jamendo.provider.JamendoContract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

abstract class TrackListHeaderAdapter extends SimpleFeedAdapter {

    private static final String PLAYLIST_FORMAT = JamendoContract.FORMAT_M3U;

    private static final String PLAYLIST_CONTENT_TYPE = JamendoContract.CONTENT_TYPE_M3U;

    private final boolean mPlaylistStreamingSupported;

    protected TrackListHeaderAdapter(Activity context, int layout, String[] from, int[] to) {
        super(context, layout, from, to);
        mPlaylistStreamingSupported = JamendoApp.isPlaylistStreamingSupported(context,
                PLAYLIST_CONTENT_TYPE);
    }

    @Override
    protected boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.link1:
                TextView link1 = (TextView) view;
                if (mPlaylistStreamingSupported) {
                    Context context = view.getContext();
                    CharSequence text = context.getText(R.string.jamendo_link_play);
                    String columnName = cursor.getColumnName(columnIndex);
                    long id = cursor.getLong(columnIndex);
                    Uri data = JamendoContract.createPlaylistUri(PLAYLIST_FORMAT, columnName, id);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(data, PLAYLIST_CONTENT_TYPE);
                    JamendoApp.setTextToLink(link1, text, intent);
                } else {
                    link1.setText("");
                }
                return true;
            case R.id.link2:
                TextView link2 = (TextView) view;
                link2.setText("");
                return true;
            default:
                return super.setViewValue(view, cursor, columnIndex);
        }
    }
}
