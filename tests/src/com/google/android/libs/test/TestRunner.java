/*-
 * Copyright (C) 2011 Google Inc.
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

package com.google.android.libs.test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.test.InstrumentationTestRunner;

/**
 * Test runner for library tests.
 */
public class TestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);

        // Ensure that AsyncTask is initialized on the main thread
        // (see http://code.google.com/p/android/issues/detail?id=18511).
        assert Looper.myLooper() == Looper.getMainLooper();
        init(AsyncTask.class);
    }

    /**
     * Initializes a class (i.e., runs any code in <code>static { ... }</code>.
     */
    private static void init(Class<?> c) {
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + c);
        }
    }
}
