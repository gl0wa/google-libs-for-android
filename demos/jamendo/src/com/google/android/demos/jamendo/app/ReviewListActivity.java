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
import com.google.android.demos.jamendo.provider.JamendoContract.Reviews;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

public class ReviewListActivity extends JamendoListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter.setDefaultImageUrl(JamendoApp.DEFAULT_USER_AVATAR_50);
    }

    @Override
    protected String[] getProjection() {
        return new String[] {
                Reviews._ID, Albums.ID, Users.IMAGE, Reviews.NAME, Reviews.RATING
        };
    }

    @Override
    protected int getLayout() {
        // TODO: Show rating as stars, not number
        return R.layout.jamendo_list_item_2;
    }

    @Override
    protected String[] getFrom() {
        return new String[] {
                Users.IMAGE, Reviews.NAME, Reviews.RATING
        };
    }

    @Override
    protected int[] getTo() {
        return new int[] {
                R.id.icon, R.id.text1, R.id.text2
        };
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        // TODO: Open review instead of album
        Object item = l.getItemAtPosition(position);
        Cursor cursor = mAdapter.getCursor();
        if (item != null && item == cursor) {
            int columnIndex = cursor.getColumnIndexOrThrow(Albums.ID);
            long albumId = cursor.getLong(columnIndex);
            Uri uri = ContentUris.withAppendedId(Albums.CONTENT_URI, albumId);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
}
