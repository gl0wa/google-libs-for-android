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
import android.content.ContextWrapper;
import android.content.Intent;

import junit.framework.Assert;

class MockContext extends ContextWrapper {

    private Intent mExpectedStartActivityIntent;

    private Intent mActualStartActivityIntent;

    public MockContext(Context base) {
        super(base);
    }

    public void expectStartActivity(Intent expectedStartActivityIntent) {
        mExpectedStartActivityIntent = expectedStartActivityIntent;
    }

    @Override
    public void startActivity(Intent intent) {
        if (mActualStartActivityIntent != null) {
            Assert.fail("Unexpected additional call to startActivity");
        }
        mActualStartActivityIntent = intent;
    }

    public void replay() {
        if (mActualStartActivityIntent != null && mExpectedStartActivityIntent == null) {
            Assert.fail("Unexpected startActivity: " + mActualStartActivityIntent);
        }
        if (mExpectedStartActivityIntent != null && mActualStartActivityIntent == null) {
            Assert.fail("Expected startActivity: " + mExpectedStartActivityIntent);
        }
        Assert.assertEquals(mExpectedStartActivityIntent, mActualStartActivityIntent);
        reset();
    }

    private void reset() {
        mExpectedStartActivityIntent = null;
        mActualStartActivityIntent = null;
    }
}
