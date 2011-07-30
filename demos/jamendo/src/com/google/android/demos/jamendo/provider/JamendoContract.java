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

import com.google.android.feeds.FeedExtras;

import android.content.Intent;
import android.net.Uri;

public final class JamendoContract implements FeedExtras {

    public static final String ACCOUNT_TYPE = "com.google.android.demos.jamendo";

    public static final String AUTH_TOKEN_TYPE = null;

    public static final Uri AUTHORITY_URI = Uri.parse("content://com.google.android.demos.jamendo");

    public static final String AUTHORITY = AUTHORITY_URI.getAuthority();

    public static final String JOIN_TRACK_ALBUM = "track_album";

    public static final String JOIN_ALBUM_ARTIST = "album_artist";

    public static final String JOIN_ALBUM_USER_STARRED = "album_user_starred";

    /**
     * An {@link Intent} extra for specifying a specific selection.
     * <p>
     * TODO: Use {@link Uri} query parameters instead?
     */
    public static final String EXTRA_SELECTION = "selection";

    /**
     * An {@link Intent} extra for specifying specific selection args.
     */
    public static final String EXTRA_SELECTION_ARGS = "selectionArgs";

    /**
     * An {@link Intent} extra for specifying a specific sort order.
     */
    public static final String EXTRA_SORT_ORDER = "sortOrder";

    /**
     * A parameter that can be appended to the selection to request a specific
     * image size.
     */
    public static final String PARAM_IMAGE_SIZE = "imagesize";

    /**
     * A parameter that can be appended to the selection multiple times to
     * specify explicit joins.
     */
    public static final String PARAM_JOIN = "join";
    
    /**
     * Specifies the maximum age of cached content.
     */
    public static final String PARAM_MAX_AGE = "max-age";
    
    /**
     * Specifies the number of items to retrieve.
     */
    public static final String PARAM_NUMBER = "n";

    // mp3 128k
    public static final String STREAM_ENCODING_MP3 = "mp31";

    // ogg vorbis q4
    public static final String STREAM_ENCODING_OGG = "mp31";

    private static final Uri BASE_TRACK_URI = Uri
            .parse("http://api.jamendo.com/get2/stream/track/redirect/");

    private static final Uri BASE_PLAYLIST_URI = Uri
            .parse("http://api.jamendo.com/get2/stream/track/");

    public static final String FORMAT_M3U = "m3u";

    public static final String FORMAT_XSPF = "xspf";

    public static final String CONTENT_TYPE_M3U = "audio/x-mpegurl";

    public static final String CONTENT_TYPE_XSPF = "application/xspf+xml";

    public static final String CONTENT_TYPE_MP3 = "audio/mpeg";

    public static Uri createTrackUri(long id, String encoding) {
        Uri.Builder builder = BASE_TRACK_URI.buildUpon();
        builder.appendQueryParameter("id", String.valueOf(id));
        builder.appendQueryParameter("streamencoding", encoding);
        return builder.build();
    }

    public static Uri createPlaylistUri(String format, String columnName, long id) {
        Uri.Builder builder = BASE_PLAYLIST_URI.buildUpon();
        builder.appendPath(format);
        builder.appendQueryParameter(columnName, String.valueOf(id));
        return builder.build();
    }

    public static Uri createRadioUri(String format, long id) {
        Uri.Builder builder = BASE_PLAYLIST_URI.buildUpon();
        builder.appendPath(format);
        builder.appendQueryParameter("order", "numradio_asc");
        builder.appendQueryParameter("radio_id", String.valueOf(id));
        return builder.build();
    }

    public static Uri createRadioUri(String format, String idstr) {
        Uri.Builder builder = BASE_PLAYLIST_URI.buildUpon();
        builder.appendPath(format);
        builder.appendQueryParameter("order", "numradio_asc");
        builder.appendQueryParameter("radio_idstr", idstr);
        return builder.build();
    }

    public static boolean isPlaylist(Uri data) {
        return data.toString().startsWith(BASE_PLAYLIST_URI.toString());
    }

    public static String getPlaylistFormat(Uri data) {
        if (isPlaylist(data)) {
            return data.getPathSegments().get(3);
        } else {
            return null;
        }
    }

    public interface Albums extends AlbumColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "albums");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.album";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.album";

        enum Order {
            /**
             * Allows you to order the albums by their date of release on
             * Jamendo.
             */
            RELEASEDATE,

            /**
             * Allows you to order the albums by their need of seeding in
             * Bittorent networks.
             */
            NEEDSEEDING,

            /**
             * Allows you to order the albums by their lack of reviews.
             */
            NEEDREVIEWS,

            /**
             * Allows you to order the albums by their average rating in
             * reviews.
             */
            RATING,

            /**
             * Allows you to order the albums by their popularity since one
             * week.
             */
            RATINGWEEK,

            /**
             * Allows you to order the albums by their popularity since one
             * month.
             */
            RATINGMONTH,

            /**
             * Allows you to order the albums by the number of playlists they
             * are in.
             */
            PLAYLISTED,

            /**
             * Allows you to order the albums by the number of playlists they
             * are in.
             */
            DOWNLOADED,

            /**
             * Allows you to order the albums by the number of times they've
             * been listened.
             */
            LISTENED,

            /**
             * Allows you to order the albums by the number of times they've
             * been starred (favourited).
             */
            STARRED,

            /**
             * In use with the relation <code>album_user_starred</code>, orders
             * by the date of starring.
             */
            STARREDDATE,

            /**
             * In use with the relation album_tag, orders by the most/least
             * relevant to the specified tag.
             */
            WEIGHT;

            public String ascending() {
                return name().toLowerCase() + "_asc";
            }

            public String descending() {
                return name().toLowerCase() + "_desc";
            }
        }
    }

    public interface Artists extends ArtistColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "artists");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.artist";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.artist";
    }

    public interface Licenses {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "licenses");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.license";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.license";
    }

    public interface Playlists extends PlaylistColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "playlists");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.playlist";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.playlist";
    }

    public interface Reviews extends ReviewColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "reviews");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.review";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.review";
    }

    public interface Tags extends TagColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "tags");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.tag";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.tag";
    }

    public interface Tracks extends TrackColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "tracks");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.track";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.track";

        enum Order {
            /**
             * Allows you to order by the number of the track in its album.
             */
            NUMALBUM,
            /**
             * Allows you to order by the number of the track in its playlist.
             */
            NUMPLAYLIST,
            /**
             * In use with the relation track_tag, orders by the most/least
             * relevant to the specified tag.
             */
            WEIGHT;

            public String ascending() {
                return name().toLowerCase() + "_asc";
            }

            public String descending() {
                return name().toLowerCase() + "_desc";
            }
        }
    }

    public interface Users extends UserColumns {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "users");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.user";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.user";
    }

    public interface Locations {
        Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "locations");

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.jamendo.location";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.jamendo.location";
    }
    
    private static long getLongQueryParameter(Uri uri, String key, long defaultValue) {
        String value = uri.getQueryParameter(key);
        return value != null ? Long.parseLong(value) : defaultValue;
    }

    private static int getIntQueryParameter(Uri uri, String key, int defaultValue) {
        String value = uri.getQueryParameter(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    private static Uri setLongQueryParameter(Uri uri, String key, long value) {
        if (0 != uri.getQueryParameters(key).size()) {
            throw new IllegalArgumentException(key + " is already specified.");
        }
        return uri.buildUpon().appendQueryParameter(key, Long.toString(value)).build();
    }

    private static Uri setIntQueryParameter(Uri uri, String key, int value) {
        if (0 != uri.getQueryParameters(key).size()) {
            throw new IllegalArgumentException(key + " is already specified.");
        }
        return uri.buildUpon().appendQueryParameter(key, Integer.toString(value)).build();
    }

    public static int getNumber(Uri uri, int defaultValue) {
        return getIntQueryParameter(uri, PARAM_NUMBER, defaultValue);
    }

    public static Uri setNumber(Uri uri, int value) {
        return setIntQueryParameter(uri, PARAM_NUMBER, value);
    }

    public static long getMaxAge(Uri uri, long defaultValue) {
        return getLongQueryParameter(uri, PARAM_MAX_AGE, defaultValue);
    }

    public static Uri setMaxAge(Uri uri, long value) {
        return setLongQueryParameter(uri, PARAM_MAX_AGE, value);
    }

    public static Uri refresh(Uri uri) {
        return setMaxAge(uri, 0);
    }

    private JamendoContract() {
    }
}
