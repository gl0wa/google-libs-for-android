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

package com.google.android.demos.jamendo.app;

import com.google.android.accounts.Account;
import com.google.android.accounts.AccountManager;
import com.google.android.accounts.AccountManagerCallback;
import com.google.android.accounts.AccountManagerFuture;
import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HomeActivity extends ListActivity implements AccountManagerCallback<Bundle> {

    private String[] mName;

    private String[] mUri;

    private String[] mOrder;

    private View mRadioView;

    private View mAccountView;

    private ListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
        int resource = R.layout.jamendo_centered_list_item_1;
        int textViewResourceId = android.R.id.text1;
        Resources resources = getResources();
        mName = resources.getStringArray(R.array.home_name);
        mUri = resources.getStringArray(R.array.home_uri);
        mOrder = resources.getStringArray(R.array.home_order);

        LayoutInflater layoutInflater = getLayoutInflater();

        mAccountView = layoutInflater.inflate(resource, null);
        TextView accountTitle = (TextView) mAccountView.findViewById(textViewResourceId);
        accountTitle.setText(R.string.jamendo_account_title);

        mRadioView = layoutInflater.inflate(resource, null);
        TextView radioTitle = (TextView) mRadioView.findViewById(textViewResourceId);
        radioTitle.setText(R.string.jamendo_radio_list_title);

        ListView listView = getListView();
        listView.addHeaderView(mAccountView, null, true);
        if (JamendoApp.isPlaylistStreamingSupported(context)) {
            listView.addHeaderView(mRadioView, null, true);
        }

        mAdapter = new ArrayAdapter<String>(context, resource, textViewResourceId, mName);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                onSearchRequested();
                return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView listView, View v, int position, long id) {
        if (v == mRadioView) {
            Intent intent = new Intent(this, RadioListActivity.class);
            startActivity(intent);
        } else if (v == mAccountView) {
            AccountManager manager = AccountManager.get(this);
            Account[] accounts = manager.getAccountsByType(JamendoContract.ACCOUNT_TYPE);
            if (accounts.length != 0) {
                // TODO: Support multiple accounts
                Account account = accounts[0];
                Uri uri = Users.CONTENT_URI;
                Uri data = uri.buildUpon().appendPath(account.name).build();
                startActivity(new Intent(Intent.ACTION_VIEW, data));
            } else {
                String[] features = {};
                Bundle addAccountOptions = null;
                AccountManagerCallback<Bundle> callback = this;
                Handler handler = new Handler();
                manager.addAccount(JamendoContract.ACCOUNT_TYPE, JamendoContract.AUTH_TOKEN_TYPE,
                        features, addAccountOptions, null, callback, handler);
            }
        } else {
            position -= listView.getHeaderViewsCount();

            Uri uri = Uri.parse(mUri[position]);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

            String order = mOrder[position];
            intent.putExtra(JamendoContract.EXTRA_SORT_ORDER, order);

            startActivity(intent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run(AccountManagerFuture<Bundle> future) {
        try {
            Bundle result = future.getResult();
            if (result.containsKey(AccountManager.KEY_INTENT)) {
                Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
