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

import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

public final class GalleryScrollListener implements AdapterView.OnItemSelectedListener {

    private final Loadable mLoadable;

    public GalleryScrollListener(Loadable loadable) {
        mLoadable = loadable;
    }

    private static int getCount(AdapterView<?> gallery) {
        // Don't use AdapterView#getCount() because it is updated asynchronously
        Adapter adapter = gallery.getAdapter();
        return adapter != null ? adapter.getCount() : 0;
    }

    private boolean isNearEnd(AdapterView<?> parent, int position) {
        return (getCount(parent) - position) < Loadable.PAGE_SIZE;
    }

    /** {@inheritDoc} */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (isNearEnd(parent, position) && mLoadable.isReadyToLoadMore()) {
            mLoadable.loadMore();
        }
    }

    /** {@inheritDoc} */
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
