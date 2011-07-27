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

interface AlbumColumns extends BaseColumns {

    /**
     * Numeric ID of the album.
     */
    String ID = "album_id";

    /**
     * Name of the album.
     * <p>
     * Example: <code>Simple Exercise</code>
     */
    String NAME = "album_name";

    /**
     * Link to the page of the album on Jamendo.
     * <p>
     * Example: <code>http://www.jamendo.com/album/33</code>
     */
    String URL = "album_url";

    /**
     * Link to the cover of the album.
     * <p>
     * Example: <code>http://img.jamendo.com/albums/33/covers/1.100.jpg</code>
     */
    String IMAGE = "album_image";

    /**
     * Total length of the album (in seconds).
     * <p>
     * Example: <code>1586</code> (seconds)
     */
    String DURATION = "album_duration";

    /**
     * Description of the album (written by the artist).
     * <p>
     * Example: <code>Rock éclectique</code>
     */
    String GENRE = "album_genre";

    /**
     * Array of dates, contains date of publication (upload), date of
     * validation, and date of release.
     * <p>
     * Example:
     * <code>"validation":"2005-05-20T12:36:20+01","release":"2004-12-28T18:46:23+01","public":"0000-00-00", "year":"2005"</code>
     */
    String DATES = "album_dates";
}
