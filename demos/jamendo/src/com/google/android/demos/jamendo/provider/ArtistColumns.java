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

interface ArtistColumns extends BaseColumns {
    /**
     * Numeric id of the artist.
     * <p>
     * Example: <code>2464</code>
     */
    String ID = "artist_id";

    /**
     * String id of the artist
     * <p>
     * Example: <code>madameolga</code>
     */
    String IDSTR = "artist_idstr";

    /**
     * Display name of the artist. different from idstr.
     * <p>
     * Example: <code>Madame Olga</code>
     */
    String NAME = "artist_name";

    /**
     * Link to the image of the artist
     * <p>
     * Example: <code>http://img.jamendo.com/artists/m/madameolga.jpg</code>
     */
    String IMAGE = "artist_image";

    /**
     * Link to the page of the artist on Jamendo.
     */
    String URL = "artist_url";

    /**
     * String id of the artist on MusicBrainz
     * <p>
     * Example: <code>0781a3f3-645c-45d1-a84f-76b4e4decf6d</code>
     */
    String MBGID = "artist_mbgid";

    /**
     * Integer id of the artist on MusicBrainz
     * <p>
     * Example: <code>263632</code>
     */
    String MBID = "artist_mbid";

    /**
     * Description of the artist (written by the artist).
     * <p>
     * Example: <code>Fusion-Rock-Electro</code>
     */
    String GENRE = "artist_genre";
}
