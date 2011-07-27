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

package com.google.android.feeds.content;

import android.content.Intent;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Indicates that the client was not authorized to make a request.
 * <p>
 * Often this type of problem can be solved by renewing the auth token using
 * {@link android.accounts.AccountManager#invalidateAuthToken(String, String)}
 * and retrying the request, although in some cases user interaction may be
 * required (for example, entering a password or completing a CAPTCHA).
 * <p>
 * Use {@link #initSolution(Intent)} or one of the constructors to specify an
 * {@link Intent} that can be used to resolve the authorization problem (e.g.,
 * re-entering a password).
 * <p>
 * This exception type is not {@link Serializable}.
 */
public class UnauthorizedException extends IOException {

    private Intent mSolution;

    private boolean mSolutionSet;

    public UnauthorizedException() {
    }

    public UnauthorizedException(String detailMessage) {
        super(detailMessage);
    }

    public UnauthorizedException(Throwable cause) {
        initCause(cause);
    }

    public UnauthorizedException(String detailMessage, Throwable cause) {
        super(detailMessage);
        initCause(cause);
    }

    public UnauthorizedException(Intent intent) {
        initSolution(intent);
    }

    public UnauthorizedException(String detailMessage, Intent solution) {
        super(detailMessage);
        initSolution(solution);
    }

    public UnauthorizedException(Throwable cause, Intent solution) {
        initCause(cause);
        initSolution(solution);
    }

    public UnauthorizedException(String detailMessage, Throwable cause, Intent intent) {
        super(detailMessage);
        initCause(cause);
        initSolution(intent);
    }

    /**
     * Initializes a solution for the problem that caused this
     * {@link UnauthorizedException}. The solution can only be initialized once.
     *
     * @throws IllegalStateException if the solution has already been
     *             initialized.
     */
    public void initSolution(Intent solution) {
        if (mSolutionSet) {
            throw new IllegalStateException("Solution has already been initialized");
        }
        mSolution = solution;
        mSolutionSet = true;
    }

    public Intent getSolution() {
        return mSolution;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // The Intent field is not serializable
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        // The Intent field is not serializable
        throw new NotSerializableException();
    }
}
