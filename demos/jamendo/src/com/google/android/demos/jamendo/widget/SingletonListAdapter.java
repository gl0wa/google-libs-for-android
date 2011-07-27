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

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

/**
 * A {@link ListAdapter} that contains a single {@link View}.
 */
public class SingletonListAdapter implements ListAdapter {

    private final int mLayout;

    private final Object mItem;

    private final boolean mEnabled;

    private final long mItemId;

    /**
     * Constructor.
     *
     * @param layout the layout to inflate in
     *            {@link #getView(int, View, ViewGroup)}
     * @param data the {@link Object} to return from {@link #getItem(int)}
     * @param itemId the ID to return from {@link #getItemId(int)}
     * @param isSelectable {@code true} if the item can be selected, {@code
     *            false} otherwise.
     */
    public SingletonListAdapter(int layout, Object data, long itemId, boolean isSelectable) {
        mLayout = layout;
        mItem = data;
        mEnabled = isSelectable;
        mItemId = itemId;
    }

    /** {@inheritDoc} */
    public boolean areAllItemsEnabled() {
        return mEnabled;
    }

    /** {@inheritDoc} */
    public boolean isEnabled(int position) {
        return mEnabled;
    }

    /** {@inheritDoc} */
    public int getCount() {
        return 1;
    }

    /** {@inheritDoc} */
    public Object getItem(int position) {
        return mItem;
    }

    /** {@inheritDoc} */
    public long getItemId(int position) {
        return mItemId;
    }

    /** {@inheritDoc} */
    public int getItemViewType(int position) {
        // This class manages its own View instance
        return AdapterView.ITEM_VIEW_TYPE_IGNORE;
    }

    /** {@inheritDoc} */
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(mLayout, parent, false);
    }

    /** {@inheritDoc} */
    public int getViewTypeCount() {
        return 1;
    }

    /** {@inheritDoc} */
    public boolean hasStableIds() {
        return mItemId > 0L;
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return false;
    }

    /** {@inheritDoc} */
    public void registerDataSetObserver(DataSetObserver observer) {
        // Data never changes
    }

    /** {@inheritDoc} */
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // Data never changes
    }
}
