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

package com.google.android.demos.jamendo.provider;

import android.provider.BaseColumns;

interface UserColumns extends BaseColumns {
    /**
     * Numeric id of the user.
     */
    String ID = "user_id";

    /**
     * String id of the user (its login)
     * <p>
     * Example: <code>"pierrotsmnrd"</code>
     */
    String IDSTR = "user_idstr";

    /**
     * Name of the user
     * <p>
     * Example: <code>"Pierre-Olivier"</code>
     */
    String NAME = "user_name";

    /**
     * Language of the user
     * <p>
     * Example: <code>"fr"</code>
     */
    String LANG = "user_lang";

    /**
     * URL of the avatar of the user
     * <p>
     * Example: <code>http://img.jamendo.com/avatars/89/4089.100.jpg</code>
     */
    String IMAGE = "user_image";

    /**
     * Array containing the date of inscription of the user
     * <p>
     * Example: <code>2005-08-30T19:50:36+01</code>
     */
    String DATES = "user_dates";
}
