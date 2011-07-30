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
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;
import com.google.android.demos.jamendo.provider.JamendoContract.Tracks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TrackListAdapter extends CursorAdapter {

    static final String LOG_TAG = "TrackListAdapter";

    public static final String[] PROJECTION = {
            Tracks._ID, Tracks.ID, Tracks.NAME, Artists.NAME, Tracks.DURATION, Tracks.URL
    };

    private final DateFormat mFormat;

    private final Date mDate;

    public TrackListAdapter(Activity context) {
        super(context, null, 0);
        mFormat = new SimpleDateFormat("m:ss");
        mDate = new Date();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_track_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindTrackNumber(view, cursor);
        bindTrackName(view, cursor);
        bindTrackDuration(view, cursor);
    }

    private void bindTrackNumber(View view, Cursor cursor) {
        TextView textView = (TextView) view.findViewById(R.id.jamendo_track_number);
        textView.setText(String.valueOf(cursor.getPosition() + 1));
    }

    private void bindTrackName(View view, Cursor cursor) {
        String trackName = cursor.getString(cursor.getColumnIndexOrThrow(Tracks.NAME));
        TextView textView = (TextView) view.findViewById(R.id.jamendo_track_name);
        textView.setText(trackName);
    }

    private void bindTrackDuration(View view, Cursor cursor) {
        long duration = cursor.getLong(cursor.getColumnIndexOrThrow(Tracks.DURATION));
        mDate.setTime(duration * 1000);
        String formatted = mFormat.format(mDate);
        TextView textView = (TextView) view.findViewById(R.id.jamendo_track_duration);
        textView.setText(formatted);
    }

    public static void playTrack(Activity activity, long trackId) {
        Uri data = JamendoContract.createTrackUri(trackId, JamendoContract.STREAM_ENCODING_MP3);
        String type = JamendoContract.CONTENT_TYPE_MP3;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(data, type);
        new RedirectHandler(activity).execute(intent);
    }
}
