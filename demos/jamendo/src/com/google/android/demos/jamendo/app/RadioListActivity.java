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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

// TODO: Add "Top radios"
public class RadioListActivity extends ListActivity {

    private String[] mArrayName;
    private String[] mArrayUri;
    private String[] mArrayType;

    private ListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
        int resource = R.layout.jamendo_centered_list_item_1;
        int textViewResourceId = android.R.id.text1;
        Resources resources = getResources();
        mArrayName = resources.getStringArray(R.array.radio_name);
        mArrayUri = resources.getStringArray(R.array.radio_uri);
        mArrayType = resources.getStringArray(R.array.radio_type);
        mAdapter = new ArrayAdapter<String>(context, resource, textViewResourceId, mArrayName);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ListView listView = getListView();
        position -= listView.getHeaderViewsCount();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse(mArrayUri[position]);
        String type = mArrayType[position];
        intent.setDataAndType(data, type);
        CharSequence title = getText(R.string.jamendo_choose_view_m3u);

        // Create a chooser in case there isn't an app to handle the intent.
        intent = Intent.createChooser(intent, title);

        startActivity(intent);
    }
}
