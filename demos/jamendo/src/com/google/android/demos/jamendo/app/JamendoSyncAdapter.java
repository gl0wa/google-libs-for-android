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

import com.google.android.accounts.AbstractSyncAdapter;
import com.google.android.accounts.Account;
import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class JamendoSyncAdapter extends AbstractSyncAdapter {

    private static void close(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    private final Context mContext;

    public JamendoSyncAdapter(Context context) {
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority) {
        syncStarredAlbums(account);
    }

    private void syncStarredAlbums(Account account) {
        // Perform the exact same query as UserActivity
        // to ensure that the XML is cached.
        //
        // TODO: Move to shared code to reduce risk of code being out-of-sync
        ContentResolver resolver = mContext.getContentResolver();
        Resources resources = mContext.getResources();
        int imageSize = resources.getDimensionPixelSize(R.dimen.thumbnail_size);
        Uri uri = JamendoContract.refresh(Albums.CONTENT_URI);
        String[] projection = {};
        String selection = String.format("%s=?&%s=?&%s=?", Users.IDSTR, JamendoContract.PARAM_JOIN,
                JamendoContract.PARAM_IMAGE_SIZE);
        String[] selectionArgs = {
                account.name, JamendoContract.JOIN_ALBUM_USER_STARRED, String.valueOf(imageSize)
        };
        String sortOrder = Albums.Order.STARREDDATE.descending();
        close(resolver.query(uri, projection, selection, selectionArgs, sortOrder));
    }
}
