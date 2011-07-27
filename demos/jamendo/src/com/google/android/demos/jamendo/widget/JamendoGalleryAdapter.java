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
import com.google.android.feeds.widget.AdapterState;
import com.google.android.imageloader.ImageLoader;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;

abstract class JamendoGalleryAdapter extends JamendoFeedAdapter {

    protected final ImageLoader mImageLoader;

    protected final Gallery mGallery;

    private final AdapterState mGalleryState;

    public JamendoGalleryAdapter(Activity context, int queryId) {
        super(context, queryId);
        mGallery = (Gallery) context.findViewById(android.R.id.list);
        mGalleryState = new AdapterState();
        mGalleryState.setAdapter(this);
        mGalleryState.setAdapterView(mGallery);
        mImageLoader = ImageLoader.get(context);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGalleryState.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mGalleryState.onRestoreInstanceState(state);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_gallery_item, parent, false);
    }

    @Override
    protected View newLoadingView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_gallery_loading, parent, false);
    }

    @Override
    protected View newErrorView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_gallery_error, parent, false);
    }
}
