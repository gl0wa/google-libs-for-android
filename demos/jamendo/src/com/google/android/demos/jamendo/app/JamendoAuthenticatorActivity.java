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
import com.google.android.accounts.AccountAuthenticatorActivity;
import com.google.android.accounts.AccountManager;
import com.google.android.accounts.ContentSyncer;
import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.provider.JamendoContract;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class JamendoAuthenticatorActivity extends AccountAuthenticatorActivity implements
        OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
        Handler.Callback {

    private static class State implements Runnable, Handler.Callback {
        private Handler mInternalHandler;

        private Handler mExternalHandler;

        private boolean mAuthenticating;

        private Context mContext;

        private String mUsername;

        private String mPassword;

        public State(Handler handler) {
            super();
            mInternalHandler = new Handler(this);
            mExternalHandler = handler;
        }

        public void setHandler(Handler handler) {
            mExternalHandler = handler;
        }

        public void cancel() {
            // Make a new handler
            mInternalHandler = new Handler(this);
            mAuthenticating = false;
        }

        public void authenticate(Context context, String username, String password) {
            if (mAuthenticating) {
                throw new IllegalStateException();
            }
            mAuthenticating = true;
            mContext = context;
            mUsername = username;
            mPassword = password;
            new Thread(this).start();
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            Message msg = mInternalHandler.obtainMessage(HANDLE_ERROR);
            msg.obj = new Exception("Unknown error");
            try {
                JamendoAuthenticator authenticator = new JamendoAuthenticator(mContext);
                Account account = new Account(mUsername, JamendoContract.ACCOUNT_TYPE);

                // TODO: Get, save, and return the authentication token

                // Add the account to the list of system accounts
                AccountManager manager = AccountManager.get(mContext);
                manager.addAccountExplicitly(account, null, null);

                ContentSyncer syncer = ContentSyncer.get(mContext);
                syncer.setIsSyncable(account, JamendoContract.AUTHORITY, 1);

                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);

                msg.what = HANDLE_DONE;
                msg.obj = result;
            } catch (RuntimeException e) {
                msg.what = HANDLE_ERROR;
                msg.obj = e;
                e.printStackTrace();
            } catch (Error e) {
                msg.what = HANDLE_ERROR;
                msg.obj = e;
                e.printStackTrace();
            } finally {
                msg.sendToTarget();
            }
        }

        private void forward(Message msg) {
            Message msg2 = mExternalHandler.obtainMessage();
            msg2.copyFrom(msg);
            msg2.sendToTarget();
        }

        /**
         * {@inheritDoc}
         */
        public boolean handleMessage(Message msg) {
            if (mInternalHandler == msg.getTarget()) {
                mAuthenticating = false;
                forward(msg);
            } else {
                // This request was canceled
            }
            return true;
        }
    }

    static final int HANDLE_DONE = 1;

    static final int HANDLE_ERROR = 2;

    static final int DIALOG_PROGRESS = 1;

    private static String getMessage(Exception e) {
        String message = e.getMessage();
        return (message != null) ? message : String.valueOf(e);
    }

    private EditText mEditUsername;

    private EditText mEditPassword;

    private Button mButtonLogin;

    private Button mButtonCancel;

    private State mState;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        setContentView(R.layout.authenticator);

        mEditUsername = (EditText) findViewById(android.R.id.text1);
        mEditPassword = (EditText) findViewById(android.R.id.text2);

        mEditPassword.setVisibility(View.GONE);

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        if (accountName != null) {
            mEditUsername.setText(accountName);
        }

        mButtonLogin = (Button) findViewById(android.R.id.button1);
        mButtonCancel = (Button) findViewById(android.R.id.button2);

        OnClickListener listener = this;
        mButtonLogin.setOnClickListener(listener);
        mButtonCancel.setOnClickListener(listener);

        mHandler = new Handler(this);

        mState = (State) getLastNonConfigurationInstance();
        if (mState != null) {
            mState.setHandler(mHandler);
        } else {
            try {
                dismissDialog(DIALOG_PROGRESS);
            } catch (IllegalArgumentException e) {
                // Ignore
            }
            mState = new State(mHandler);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mState;
    }

    private void onError(CharSequence message) {
        Context context = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, this);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onLoginComplete(Bundle result) {
        setAccountAuthenticatorResult(result);
        setResult(RESULT_OK);
        finish();
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLE_DONE:
                dismissDialog(DIALOG_PROGRESS);
                Bundle result = (Bundle) msg.obj;
                onLoginComplete(result);
                return true;
            case HANDLE_ERROR:
                dismissDialog(DIALOG_PROGRESS);
                String message = getMessage((Exception) msg.obj);
                onError(message);
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {
        if (v == mButtonLogin) {
            onLoginButton();
        } else if (v == mButtonCancel) {
            onCancelButton();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                Context context = this;
                CharSequence message = getText(R.string.jamendo_message_authenticating);
                ProgressDialog dialog = new ProgressDialog(context);
                dialog.setMessage(message);
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(this);
                return dialog;
            default:
                return super.onCreateDialog(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(DialogInterface dialog, int which) {
        // Pass
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(DialogInterface dialog) {
        mState.cancel();
    }

    private void onLoginButton() {
        showDialog(DIALOG_PROGRESS);
        Context context = this;
        String username = mEditUsername.getText().toString();
        String password = mEditPassword.getText().toString();
        mState.authenticate(context, username, password);
    }

    private void onCancelButton() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
