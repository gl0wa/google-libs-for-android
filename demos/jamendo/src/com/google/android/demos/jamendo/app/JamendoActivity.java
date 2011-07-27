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
import com.google.android.demos.jamendo.widget.CompositeListAdapter;
import com.google.android.demos.jamendo.widget.AdapterStateObserver;
import com.google.android.feeds.widget.AdapterState;
import com.google.android.feeds.widget.BaseFeedAdapter;
import com.google.android.imageloader.ImageLoader;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ListAdapter;

public abstract class JamendoActivity extends ListActivity {

    protected static final int QUERY_HEADER = 1;

    protected static final int QUERY_LIST = 2;

    protected ImageLoader mImageLoader;

    protected BaseFeedAdapter mHeaderAdapter;

    protected ListAdapter mSeparatorAdapter;

    protected BaseFeedAdapter mListAdapter;

    private AdapterState mListState;

    protected abstract BaseFeedAdapter createHeaderAdapter();

    protected abstract ListAdapter createSeparatorAdapter();

    protected abstract BaseFeedAdapter createListAdapter();

    protected abstract void changeIntent(Intent intent);

    protected final String getDimensionPixelSizeAsString(int resId) {
        Resources resources = getResources();
        int size = resources.getDimensionPixelSize(resId);
        return Integer.toString(size);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.jamendo_list);

        mImageLoader = ImageLoader.get(this);
        mHeaderAdapter = createHeaderAdapter();
        mSeparatorAdapter = createSeparatorAdapter();
        mListAdapter = createListAdapter();
        ListAdapter listAdapter = new CompositeListAdapter(mHeaderAdapter, mSeparatorAdapter, mListAdapter);
        setListAdapter(listAdapter);

        AdapterStateObserver observer = new AdapterStateObserver();
        observer.setActivity(this);
        observer.setAdapters(mHeaderAdapter, mListAdapter);
        
        mListState = new AdapterState();
        mListState.setAdapterView(getListView());
        mListState.setAdapter(getListAdapter());

        if (savedInstanceState == null) {
            changeIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mHeaderAdapter.onSaveInstanceState(outState);
        mListAdapter.onSaveInstanceState(outState);
        mListState.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mHeaderAdapter.onRestoreInstanceState(state);
        mListAdapter.onRestoreInstanceState(state);
        mListState.onRestoreInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHeaderAdapter.onResume();
        mListAdapter.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHeaderAdapter.onStop();
        mListAdapter.onStop();
    }
}
