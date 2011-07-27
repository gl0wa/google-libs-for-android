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

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.Adapter;
import android.widget.AdapterView;

/**
 * Saves the state of an {@link AdapterView} just before the data is unloaded
 * and waits until after the data has been reloaded to restore the state.
 * <p>
 * Clients must invoke the life-cycle methods
 * {@link #onSaveInstanceState(Bundle)} and
 * {@link #onRestoreInstanceState(Bundle)}.
 */
public class AdapterState {

    private static final String STATE_VIEWS = "feeds:views";

    private static SparseArray<Parcelable> getOrCreateSparseParcelableArray(Bundle bundle,
            String key) {
        SparseArray<Parcelable> array = bundle.getSparseParcelableArray(key);
        if (array == null) {
            array = new SparseArray<Parcelable>();
            bundle.putSparseParcelableArray(key, array);
        }
        return array;
    }

    private final DataSetObserver mObserver;

    final SparseArray<Parcelable> mState;

    AdapterView<?> mView;

    private Adapter mAdapter;

    public AdapterState() {
        mState = new SparseArray<Parcelable>(1);
        mObserver = new AdapterViewObserver();
    }

    public void setAdapterView(AdapterView<?> view) {
        mView = view;
    }

    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
            mAdapter = null;
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mObserver);
        }
    }

    /**
     * Handles the {@link Activity#onSaveInstanceState(Bundle)} callback.
     * 
     * @param outState the output state.
     */
    public void onSaveInstanceState(Bundle outState) {
        SparseArray<Parcelable> views = getOrCreateSparseParcelableArray(outState, STATE_VIEWS);
        if (mView.getCount() != 0) {
            // Case 1: onSaveInstanceState was called
            // before the data was invalidated
            mView.saveHierarchyState(mState);
        } else {
            // Case 2: onSaveInstanceState was called
            // after the data was invalidated
        }
        views.put(mView.getId(), mState.get(mView.getId()));

    }

    /**
     * Handles the {@link Activity#onRestoreInstanceState(Bundle)} callback.
     * 
     * @param state the saved state.
     */
    public void onRestoreInstanceState(Bundle state) {
        SparseArray<Parcelable> views = state.getSparseParcelableArray(STATE_VIEWS);
        mState.clear();
        mState.put(mView.getId(), views.get(mView.getId()));
    }

    boolean hasSavedState() {
        return 0 != mState.size();
    }

    private class AdapterViewObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (hasSavedState()) {
                mView.restoreHierarchyState(mState);
                mState.clear();
            } else {
                // Already restored
            }
        }

        @Override
        public void onInvalidated() {
            if (!hasSavedState()) {
                mView.saveHierarchyState(mState);
            } else {
                // Already saved
            }
        }
    }
}
