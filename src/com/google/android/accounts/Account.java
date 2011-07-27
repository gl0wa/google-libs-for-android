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

package com.google.android.accounts;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Mirrors {@link android.accounts.Account}
 */
public class Account implements Parcelable {

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        /**
         * {@inheritDoc}
         */
        public Account createFromParcel(Parcel source) {
            return new Account(source);
        }

        /**
         * {@inheritDoc}
         */
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    /**
     * Mirrors {@link android.accounts.Account#name}
     */
    public String name;

    /**
     * Mirrors {@link android.accounts.Account#type}
     */
    public String type;

    /**
     * Mirrors {@link android.accounts.Account#Account(Parcel)}
     */
    public Account(Parcel in) {
        this.name = in.readString();
        this.type = in.readString();
    }

    /**
     * Mirrors {@link android.accounts.Account#Account(String, String)}
     */
    @SuppressWarnings("hiding")
    public Account(String name, String type) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("the name must not be empty: " + name);
        }
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("the type must not be empty: " + type);
        }
        this.name = name;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }
        final Account other = (Account) o;
        return name.equals(other.name) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Account {name=" + name + ", type=" + type + "}";
    }

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(type);
    }
}
