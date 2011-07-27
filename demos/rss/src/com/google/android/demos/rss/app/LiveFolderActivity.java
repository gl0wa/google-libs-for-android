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

package com.google.android.demos.rss.app;

import com.google.android.demos.rss.R;
import com.google.android.demos.rss.provider.RssContract.Items;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.LiveFolders;
import android.view.View;
import android.widget.ListView;

public class LiveFolderActivity extends ListActivity {

    private static Intent createLiveFolder(Context context, Uri uri, String name) {
        Parcelable icon = Intent.ShortcutIconResource.fromContext(context, R.drawable.rss_icon);

        Intent intent = new Intent();
        intent.setData(uri);
        intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, name);
        intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, icon);
        intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST);
        return intent;
    }

    private String[] mUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use layout pre-populated with feed names
        setContentView(R.layout.live_folders);

        // Load URLs for feeds
        Resources resources = getResources();
        mUrls = resources.getStringArray(R.array.urls);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String name = (String) l.getItemAtPosition(position);
        String url = mUrls[position];
        Uri uri = Items.liveFolderUri(url);
        Intent data = createLiveFolder(this, uri, name);
        setResult(RESULT_OK, data);
        finish();
    }
}
