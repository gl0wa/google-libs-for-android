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

import com.google.android.accounts.AbstractSyncAdapter;
import com.google.android.accounts.AbstractSyncService;
import com.google.android.demos.jamendo.R;

import android.content.Context;
import android.os.Process;

public class JamendoSyncAdapterService extends AbstractSyncService {

    private static final String LOG_TAG = "JamendoSync";

    private static final int NOTIFICATION_ID = 1;

    public JamendoSyncAdapterService() {
        super(LOG_TAG, Process.THREAD_PRIORITY_BACKGROUND, NOTIFICATION_ID);
    }

    @Override
    protected AbstractSyncAdapter createSyncAdapter() {
        Context context = this;
        return new JamendoSyncAdapter(context);
    }

    @Override
    protected CharSequence createNotificationTitle() {
        return getText(R.string.jamendo_title);
    }

    @Override
    protected CharSequence createNotificationText() {
        return getText(R.string.jamendo_service_sync_text);
    }
}
