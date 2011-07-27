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
import com.google.android.feeds.widget.DecoratedFeedAdapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A {@link DecoratedQueryAdapter} for Jamendo.
 * <ul>
 * <li>Loads, unloads, reloads, and closes a {@link Cursor} in response to
 * {@link Activity} life-cycle events</li>
 * <li>Saves and restores the content provider query parameters</li>
 * <li>Displays a progress spinner when more rows are loading</li>
 * <li>Displays an error message when additional rows fail to load</li>
 * <li>Listens for retry button clicks</li>
 * <li>Provides convenience methods for managing queries</li>
 * </ul>
 */
public abstract class JamendoFeedAdapter extends DecoratedFeedAdapter implements
        View.OnClickListener {

    public JamendoFeedAdapter(Context context, int queryId) {
        super(context, queryId);
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

    @Override
    protected View newLoadingView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.jamendo_footer_loading, parent, false);
    }

    @Override
    protected View newErrorView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.jamendo_footer_error, parent, false);
        view.findViewById(R.id.retry).setOnClickListener(this);
        return view;
    }
}
