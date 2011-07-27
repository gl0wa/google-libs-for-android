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

interface PlaylistColumns extends BaseColumns {
    /**
     * Numeric id of the playlist.
     */
    String ID = "playlist_id";

    /**
     * Name of the playlist.
     * <p>
     * Example: <code>"Playlists2007_iLikeIt"</code>
     */
    String NAME = "playlist_name";

    /**
     * Length of the playlist (in seconds).
     * <p>
     * Example: <code>310</code> (seconds)
     */
    String DURATION = "playlist_duration";
}
