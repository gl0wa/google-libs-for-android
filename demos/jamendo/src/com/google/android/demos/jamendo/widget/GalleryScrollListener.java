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

import com.google.android.feeds.widget.FeedAdapter;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

public class GalleryScrollListener implements OnItemSelectedListener {

    private final FeedAdapter mAdapter;

    public GalleryScrollListener(FeedAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * {@inheritDoc}
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int count = mAdapter.getCount();
        if (position > count - 10) {
            if (mAdapter.isReadyToLoadMore()) {
                mAdapter.loadMore(20);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
