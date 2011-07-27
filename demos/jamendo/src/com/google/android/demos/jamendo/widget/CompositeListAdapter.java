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

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * Combines multiple views/adapters into a single {@link ListAdapter}.
 * <p>
 * The data set is not valid unless all of the child data sets are valid.
 */
public class CompositeListAdapter extends BaseAdapter {

    private final ListAdapter[] mAdapters;

    private final boolean[] mDataValid;

    /**
     * Constructs a {@link CompositeListAdapter}.
     *
     * @param adapters the adapters to compose. The creator is responsible for
     *            ensuring that the values returned by
     *            {@link ListAdapter#getItemId(int)} do not conflict. Empty
     *            child adapters are invalid until
     *            {@link DataSetObserver#onChanged()} is called.
     * @throws NullPointerException if the array or any of its elements are
     *             {@code null}.
     */
    public CompositeListAdapter(ListAdapter... adapters) {
        if (adapters == null) {
            throw new NullPointerException("Adapter array is null");
        }
        mAdapters = adapters;
        mDataValid = new boolean[adapters.length];
        for (int index = 0; index < adapters.length; index++) {
            ListAdapter adapter = adapters[index];
            if (adapter == null) {
                throw new NullPointerException("Adapter at index " + index + " is null");
            }
            mDataValid[index] = !adapter.isEmpty();
            adapter.registerDataSetObserver(new ChildDataSetObserver(index));
        }
    }

    private boolean isDataValid() {
        for (boolean dataValid : mDataValid) {
            if (!dataValid) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    public int getCount() {
        if (!isDataValid()) {
            return 0;
        }
        int count = 0;
        for (ListAdapter adapter : mAdapters) {
            count += adapter.getCount();
        }
        return count;
    }

    @Override
    public final boolean isEmpty() {
        int count = getCount();
        return count == 0;
    }

    @Override
    public boolean hasStableIds() {
        for (ListAdapter adapter : mAdapters) {
            if (!adapter.hasStableIds()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        for (ListAdapter adapter : mAdapters) {
            if (!adapter.areAllItemsEnabled()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        for (ListAdapter adapter : mAdapters) {
            int count = adapter.getCount();
            if (position < count) {
                return adapter.isEnabled(position);
            }
            position -= count;
        }
        throw new IndexOutOfBoundsException();
    }

    /** {@inheritDoc} */
    public Object getItem(int position) {
        for (ListAdapter adapter : mAdapters) {
            int count = adapter.getCount();
            if (position < count) {
                return adapter.getItem(position);
            }
            position -= count;
        }
        return null;
    }

    /** {@inheritDoc} */
    public long getItemId(int position) {
        for (ListAdapter adapter : mAdapters) {
            int count = adapter.getCount();
            if (position < count) {
                return adapter.getItemId(position);
            }
            position -= count;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public View getView(int position, View convertView, ViewGroup parent) {
        for (ListAdapter adapter : mAdapters) {
            int count = adapter.getCount();
            if (position < count) {
                return adapter.getView(position, convertView, parent);
            }
            position -= count;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getItemViewType(int position) {
        int viewTypeTotal = 0;
        for (ListAdapter adapter : mAdapters) {
            int count = adapter.getCount();
            if (position < count) {
                int itemViewType = adapter.getItemViewType(position);
                if (itemViewType < 0) {
                    // AdapterView.ITEM_VIEW_TYPE_IGNORE or
                    // ITEM_VIEW_TYPE_HEADER_OR_FOOTER
                    return itemViewType;
                } else {
                    // Map view types, for example:
                    // [0, 1, 2] [0, 1, 2] -> [0, 1, 2, 3, 4, 5].
                    return viewTypeTotal + itemViewType;
                }
            }
            position -= count;
            viewTypeTotal += adapter.getViewTypeCount();
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getViewTypeCount() {
        int count = 0;
        for (ListAdapter adapter : mAdapters) {
            count += adapter.getViewTypeCount();
        }

        // Must return a value greater than or equal to 1
        return Math.max(count, 1);
    }

    private class ChildDataSetObserver extends DataSetObserver {

        private final int mIndex;

        public ChildDataSetObserver(int index) {
            mIndex = index;
        }

        @Override
        public void onChanged() {
            mDataValid[mIndex] = true;
            if (isDataValid()) {
                notifyDataSetChanged();
            } else {
                // One of the other adapters is invalid
            }
        }

        @Override
        public void onInvalidated() {
            // Only send a notification if the data was previously valid
            boolean notify = isDataValid();

            mDataValid[mIndex] = false;
            if (notify) {
                notifyDataSetInvalidated();
            }
        }
    }
}
