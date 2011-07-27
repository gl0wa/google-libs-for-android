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

package com.google.android.feeds.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * An adapter that decorates the item data with an extra row containing a
 * loading indicator whenever more rows are being loaded.
 * <p>
 * If loading fails, the additional row is replaced with an error message.
 */
public abstract class DecoratedFeedAdapter extends BaseFeedAdapter {

    protected DecoratedFeedAdapter(Context context, int id) {
        super(context, id);
    }
    
    private int mCount;

    @Override
    public int getCount() {
        int count = super.getCount();
        if (hasStatusView()) {
            count += 1;
        }
        return count;
    }

    @Override
    public final boolean isEmpty() {
        // Don't allow subclasses to override this
        // method to ensure consistency.
        return super.isEmpty();
    }

    /**
     * Returns {@code true} if the adapter is currently showing an additional
     * row containing the adapter status, {@code false} otherwise.
     */
    protected final boolean hasStatusView() {
        switch (super.getCount()) {
            case 0:
                // The status view is only shown when the adapter has data
                return false;
            default:
                return isLoadingMore() || hasError();
        }
    }

    /**
     * Returns {@code true} if the specified position displays the adapter
     * status, {@code false} otherwise.
     *
     * @see #newLoadingView(Context, ViewGroup)
     * @see #newErrorView(Context, ViewGroup)
     */
    protected final boolean isStatusView(int position) {
        return hasStatusView() && position == lastPosition();
    }

    /**
     * Returns the last position or {@code -1} if the adapter is empty.
     */
    private int lastPosition() {
        int count = getCount();
        return count - 1;
    }
    
    @Override
    public Object getItem(int position) {
        if (isStatusView(position)) {
            return null;
        } else {
            return super.getItem(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if (isStatusView(position)) {
            return AdapterView.INVALID_ROW_ID;
        } else {
            return super.getItemId(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isStatusView(position)) {
            // For simplicity, don't recycle the loading/error views
            return IGNORE_ITEM_VIEW_TYPE;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isStatusView(position)) {
            Context context = parent.getContext();
            if (isLoadingMore()) {
                return newLoadingView(context, parent);
            } else {
                return newErrorView(context, parent);
            }
        } else {
            return super.getView(position, convertView, parent);
        }
    }
    
    @Override
    protected void onQueryChanged() {
        super.onQueryChanged();
        
        // Check if the query change caused the number of items to change
        if (isDataValid() && countChanged()) {
            notifyDataSetChanged();
        }
    }

    @Override
    protected void onQueryStateChanged() {
        super.onQueryStateChanged();

        // Check if the query state change caused the number of items to change
        if (isDataValid() && countChanged()) {
            notifyDataSetChanged();
        }
    }

    /**
     * Returns {@code true} if the value of {@link #getCount()} has changed
     * since the last call to {@link #notifyDataSetChanged()}, {@code false}
     * otherwise.
     */
    private boolean countChanged() {
        return mCount != getCount();
    }
    
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mCount = getCount();
    }
    
    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();
        mCount = getCount();
        if (mCount != 0) {
            throw new IllegalStateException("Count must be zero when data set is invalid");
        }
    }

    /**
     * Creates a {@link View} that is shown at the end of the list if an error
     * occurs while loading more rows.
     * <p>
     * The view may include a button for retrying the failed request.
     */
    protected abstract View newErrorView(Context context, ViewGroup parent);

    /**
     * Creates a {@link View} that is shown while additional rows are being
     * loaded.
     */
    protected abstract View newLoadingView(Context context, ViewGroup parent);
}
