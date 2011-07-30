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

package com.google.android.demos.atom.app;

import com.google.android.demos.atom.R;
import com.google.android.demos.atom.provider.AtomContract.Entries;
import com.google.android.demos.atom.provider.AtomContract.Feeds;
import com.google.android.demos.atom.widget.EntryDialog;
import com.google.android.demos.atom.widget.FeedAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import java.text.MessageFormat;

/**
 * A feed activity loads and displays a list of items in an Atom feed.
 * <p>
 * Feed entries are displayed in a dialog managed by the feed for efficiency.
 */
public class FeedActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final int DIALOG_ENTRY = 1;

    private static final int DIALOG_INVALID_URL = 2;

    private static final int LOADER_ENTRIES = 1;

    /**
     * Preference key for the URL of the last accessed feed. This is a really
     * simple Atom viewer that only remembers one subscription.
     */
    private static final String PREFERENCE_FEED = "feed";

    /**
     * A default feed to use when the application is started for the first time.
     */
    private static final String DEFAULT_FEED = "http://www.atomenabled.org/atom.xml";

    private FeedAdapter mAdapter;

    private EntryDialog mEntryDialog;

    private SharedPreferences mPreferences;

    private String mInvalidUrl;
    
    private View mEmpty;

    private View mLoading;

    private View mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.list_activity);
        ListView listView = (ListView) findViewById(android.R.id.list);
        mEmpty = findViewById(R.id.empty);
        mLoading = findViewById(R.id.loading);
        mError = findViewById(R.id.error);
        mError.findViewById(R.id.retry).setOnClickListener(this);

        // Loader from last Activity instance may still be loading
        mLoading.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = new FeedAdapter(this);
        mAdapter.registerDataSetObserver(new TitleObserver());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        getSupportLoaderManager().initLoader(LOADER_ENTRIES, Bundle.EMPTY, this);
    }
    
    private Uri getContentUri() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        String type = intent.resolveType(this);
        if (Entries.CONTENT_TYPE.equals(type)) {
            return data;
        } else if ("application/atom+xml".equalsIgnoreCase(type)) {
            return Entries.contentUri(data.toString());
        } else {
            String channelUrl = mPreferences.getString(PREFERENCE_FEED, DEFAULT_FEED);
            return Entries.contentUri(channelUrl);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mLoading.setVisibility(mAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        setWindowIndeterminateProgressVisible(!mAdapter.isEmpty());
        Context context = this;
        Uri uri = getContentUri();
        return FeedAdapter.createLoader(context, uri);
    }
    
    /**
     * {@inheritDoc}
     */
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(mAdapter.isEmpty() && !mAdapter.hasError() ? View.VISIBLE : View.GONE);
        mError.setVisibility(mAdapter.isEmpty() && mAdapter.hasError() ? View.VISIBLE : View.GONE);
        setWindowIndeterminateProgressVisible(false);
    }

    /**
     * {@inheritDoc}
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        setWindowIndeterminateProgressVisible(false);
    }
    
    private void setWindowIndeterminateProgressVisible(boolean value) {
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                value ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This method is called when the search interface is used to specify a
        // different feed URL. The same activity instance is reused, so it needs
        // to be updated to show a different feed. You don't need to worry about
        // implementing this method unless you have set launchMode="singleTop".
        setIntent(intent);
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            String feedUrl = intent.getStringExtra(SearchManager.QUERY);
            if (Feeds.isValidFeedUrl(feedUrl)) {
                // Save this feed URL in the preferences, so that it will be
                // opened the next time the application is started.
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(PREFERENCE_FEED, feedUrl);
                editor.commit();
            } else {
                mInvalidUrl = feedUrl;
                showDialog(DIALOG_INVALID_URL);
            }
        }
        reload();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ENTRY) {
            mEntryDialog = new EntryDialog(this);
            return mEntryDialog;
        } else if (id == DIALOG_INVALID_URL) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.atom_title);
            builder.setMessage(R.string.atom_error_invalid_url);
            builder.setPositiveButton(R.string.atom_button_ok, null);
            builder.setCancelable(true);
            return builder.create();
        } else {
            return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (id == DIALOG_INVALID_URL) {
            AlertDialog d = (AlertDialog) dialog;
            String template = getString(R.string.atom_error_invalid_url);
            CharSequence message = MessageFormat.format(template, mInvalidUrl);
            d.setMessage(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Object item = l.getItemAtPosition(position);
        Cursor cursor = mAdapter.getCursor();
        if (item != null && item == cursor) {
            showDialog(DIALOG_ENTRY);
            FeedAdapter.setEntryData(mEntryDialog, cursor);
            mEntryDialog.scrollToTop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feed_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                reload();
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry:
                reload();
                break;
        }
    }

    private void reload() {
        getSupportLoaderManager().restartLoader(LOADER_ENTRIES, Bundle.EMPTY, this);
    }

    private final class TitleObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            Cursor cursor = mAdapter.getCursor();
            Bundle extras = cursor.getExtras();
            String title = extras.getString(Feeds.TITLE_PLAINTEXT);
            if (title != null) {
                setTitle(title);
            }
        }
    }
}
