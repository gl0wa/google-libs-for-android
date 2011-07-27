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

interface ReviewColumns extends BaseColumns {
    /**
     * Numeric id of the review.
     */
    String ID = "review_id";

    /**
     * Title of the review.
     * <p>
     * Example: <code>"this album is wonderful !!"</code>
     */
    String NAME = "review_name";

    /**
     * Text of the review.
     * <p>
     * Example: <code>"yes, really, this album is wonderful"</code>
     */
    String TEXT = "review_text";

    /**
     * Text of the review. A number between 0 and 10.
     */
    String RATING = "review_rating";

    /**
     * Language the review is written in. A number between 0 and 10.
     */
    String LANG = "review_lang";

    /**
     * Array containing the date when the review has been added, and when it has
     * been updated for the last time.
     * <p>
     * Example: <code>"2005-08-30T19:50:36+01"</code>
     */
    String DATES = "review_dates";
}
