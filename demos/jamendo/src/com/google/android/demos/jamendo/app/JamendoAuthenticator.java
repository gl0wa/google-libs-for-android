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

import com.google.android.accounts.DatabaseAuthenticator;
import com.google.android.demos.jamendo.R;

import android.content.Context;

public class JamendoAuthenticator extends DatabaseAuthenticator {

    private static final String DATABASE_NAME = "accounts";

    public JamendoAuthenticator(Context context) {
        super(context, DATABASE_NAME, JamendoAuthenticatorActivity.class);
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Context context = getContext();
        return context.getString(R.string.jamendo_auth_token_label);
    }
}
