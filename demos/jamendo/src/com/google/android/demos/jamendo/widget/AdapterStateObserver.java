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
import com.google.android.feeds.widget.FeedAdapter;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.Window;

/**
 * Computes the aggregate query state of one or more adapters.
 * <p>
 * Shows a loading banner when loading.
 * <p>
 * Shows a title bar spinner when reloading.
 * <p>
 * Shows an error message and retry button when there is an error.
 */
public class AdapterStateObserver {
    
    private static void setViewVisible(View view, boolean visible) {
        if (view != null) {
            int visibility = visible ? View.VISIBLE : View.GONE;
            if (visibility != view.getVisibility()) {
                view.setVisibility(visibility);
            }
        }
    }

    private static void setWindowIndeterminateProgressVisible(Window window, boolean visible) {
        int featureId = Window.FEATURE_INDETERMINATE_PROGRESS;
        int value = visible ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF;
        window.setFeatureInt(featureId, value);
    }
    
    private final Observer mObserver = new Observer();

    private View mLoading;

    private View mError;

    private Window mWindow;

    private FeedAdapter[] mAdapters;
    
    public void setAdapters(FeedAdapter... adapters) {
        if (mAdapters != null) {
            for (FeedAdapter adapter : mAdapters) {
                adapter.unregisterDataSetObserver(mObserver);
                adapter.unregisterOnQueryStateChangeListener(mObserver);
            }
        }
        mAdapters = adapters;
        if (mAdapters != null) {
            for (FeedAdapter adapter : mAdapters) {
                adapter.registerDataSetObserver(mObserver);
                adapter.registerOnQueryStateChangeListener(mObserver);
            }
        }
    }
    
    public void setActivity(Activity activity) {
        if (activity != null) {
            mLoading = activity.findViewById(R.id.loading);
            mError = activity.findViewById(R.id.error);
            mWindow = activity.getWindow();
            mError.findViewById(R.id.retry).setOnClickListener(mObserver);
        } else {
            mLoading = null;
            mError = null;
            mWindow = null;
            mError = null;
        }
    }

    public boolean hasCursor() {
        for (FeedAdapter adapter : mAdapters) {
            if (!adapter.hasData()) {
                return false;
            }
        }
        return true;
    }

    public boolean isQueryDone() {
        for (FeedAdapter adapter : mAdapters) {
            if (!adapter.isQueryDone()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasError() {
        for (FeedAdapter adapter : mAdapters) {
            if (adapter.hasError()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        for (FeedAdapter adapter : mAdapters) {
            if (0 != adapter.getCount()) {
                return false;
            }
        }
        return true;
    }

    public void retry() {
        for (FeedAdapter adapter : mAdapters) {
            adapter.retry();
        }
    }

    private void updateViews() {
        setViewVisible(mLoading, false);
        setViewVisible(mError, false);
        setWindowIndeterminateProgressVisible(mWindow, false);
        boolean isLoaded = hasCursor() && isQueryDone();
        if (isLoaded) {
            if (isEmpty()) {
                if (hasError()) {
                    setViewVisible(mError, true);
                }
            }
        } else {
            if (isEmpty()) {
                setViewVisible(mLoading, true);
            } else {
                setWindowIndeterminateProgressVisible(mWindow, true);
            }
        }
    }
    
    private class Observer extends DataSetObserver implements
            FeedAdapter.OnQueryStateChangeListener, View.OnClickListener {

        /**
         * {@inheritDoc}
         */
        public void onQueryStateChange(FeedAdapter adapter) {
            updateViews();
        }

        @Override
        public void onChanged() {
            updateViews();
        }

        @Override
        public void onInvalidated() {
            updateViews();
        }

        /**
         * {@inheritDoc}
         */
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.retry:
                    retry();
                    break;
            }
        }
    };
}
