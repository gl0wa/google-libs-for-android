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

import com.google.android.imageloader.ImageLoader;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SimpleFeedAdapter extends JamendoFeedAdapter {

    private final int mLayout;

    private final String[] mFrom;

    private final int[] mTo;
    
    private final ImageLoader mImageLoader;

    private String mDefaultImageUrl;

    public SimpleFeedAdapter(Activity context, int queryId, int layout, String[] from, int[] to) {
        super(context, queryId);
        mLayout = layout;
        mFrom = from;
        mTo = to;
        mImageLoader = ImageLoader.get(context);
    }

    public void setDefaultImageUrl(String defaultImage) {
        mDefaultImageUrl = defaultImage;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(mLayout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        for (int i = 0; i < mFrom.length; i++) {
            final View v = view.findViewById(mTo[i]);
            if (v != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(mFrom[i]);
                if (!setViewValue(v, cursor, columnIndex)) {
                    String value = cursor.getString(columnIndex);
                    if (value == null) {
                        value = "";
                    }
                    if (v instanceof TextView) {
                        TextView textView = (TextView) v;
                        textView.setText(value);
                    } else if (v instanceof ImageView) {
                        ImageView imageView = (ImageView) v;
                        try {
                            imageView.setImageResource(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            if (TextUtils.isEmpty(value)) {
                                value = mDefaultImageUrl;
                            }
                            mImageLoader.bind(this, imageView, value);
                        }
                    } else {
                        throw new IllegalStateException(v.getClass().getName() + " is not a "
                                + " view that can be bound by this " + getClass().getSimpleName());
                    }
                }
            }
        }
    }

    protected boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return false;
    }
}
