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
import com.google.android.imageloader.ImageLoader;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchAdapter extends CursorAdapter {
    
    private final ImageLoader mImageLoader;

    public SearchAdapter(Context context) {
        super(context, null, 0);
        mImageLoader = ImageLoader.get(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_list_item_2, parent, false);
    }

    private void bindTextView(View view, Cursor cursor, int viewId, String columnName) {
        TextView textView = (TextView) view.findViewById(viewId);
        int columnIndex = cursor.getColumnIndexOrThrow(columnName);
        textView.setText(cursor.getString(columnIndex));
    }

    private void bindImageView(View view, Cursor cursor, int viewId, String columnName) {
        ImageView imageView = (ImageView) view.findViewById(viewId);
        int columnIndex = cursor.getColumnIndexOrThrow(columnName);
        String url = cursor.getString(columnIndex);
        switch (mImageLoader.bind(this, imageView, url)) {
            case LOADING:
            case ERROR:
                imageView.setImageDrawable(null);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindTextView(view, cursor, R.id.text1, SearchManager.SUGGEST_COLUMN_TEXT_1);
        bindTextView(view, cursor, R.id.text2, SearchManager.SUGGEST_COLUMN_TEXT_2);
        bindImageView(view, cursor, R.id.icon, SearchManager.SUGGEST_COLUMN_ICON_2);
    }
}
