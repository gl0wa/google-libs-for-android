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
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

import java.text.MessageFormat;

/**
 * A feed activity loads and displays a list of items in an Atom feed.
 * <p>
 * Feed entries are displayed in a dialog managed by the feed for efficiency.
 */
public class FeedActivity extends ListActivity {

    private static final int DIALOG_ENTRY = 1;

    private static final int DIALOG_INVALID_URL = 2;

    private static final int QUERY_ENTRIES = 1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.list_activity);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = new FeedAdapter(this, QUERY_ENTRIES);

        // Configure the ListActivity to use the feed adapter,
        // not the inner adapter (using the inner adapter directly would work,
        // but would not provide status information or a way
        // for the user to retry their request).
        setListAdapter(mAdapter);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        String feedUrl = uri != null ? Entries.getFeedUrl(uri) : mPreferences.getString(
                PREFERENCE_FEED, DEFAULT_FEED);
        mAdapter.changeFeedUrl(feedUrl);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mAdapter.onRestoreInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This method is called when the search interface is used to specify a
        // different feed URL. The same activity instance is reused, so it needs
        // to be updated to show a different feed. You don't need to worry about
        // implementing this method unless you have set launchMode="singleTop".
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            String feedUrl = intent.getStringExtra(SearchManager.QUERY);
            if (Feeds.isValidFeedUrl(feedUrl)) {
                // Save this feed URL in the preferences, so that it will be
                // opened the next time the application is started.
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(PREFERENCE_FEED, feedUrl);
                editor.commit();

                mAdapter.changeFeedUrl(feedUrl);
            } else {
                mInvalidUrl = feedUrl;
                showDialog(DIALOG_INVALID_URL);
            }
        }
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
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
                mAdapter.refresh();
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
