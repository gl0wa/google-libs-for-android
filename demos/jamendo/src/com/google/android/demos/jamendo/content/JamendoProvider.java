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

package com.google.android.demos.jamendo.content;

import com.google.android.demos.jamendo.net.JamendoCache;
import com.google.android.demos.jamendo.net.QueryBuilder;
import com.google.android.demos.jamendo.provider.JamendoContract;
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.demos.jamendo.provider.JamendoContract.Artists;
import com.google.android.demos.jamendo.provider.JamendoContract.Playlists;
import com.google.android.demos.jamendo.provider.JamendoContract.Reviews;
import com.google.android.demos.jamendo.provider.JamendoContract.Tags;
import com.google.android.demos.jamendo.provider.JamendoContract.Tracks;
import com.google.android.demos.jamendo.provider.JamendoContract.Users;
import com.google.android.feeds.content.FeedLoader;
import com.google.android.feeds.content.FeedProvider;
import com.google.android.feeds.provider.FeedUri;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;

import java.net.ContentHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JamendoProvider extends ContentProvider {

    private static final String TAG = "JamendoProvider";

    private static final Uri BASE_URI = Uri.parse("http://api.jamendo.com/get2/");

    private static final int PAGE_SIZE = 20;

    private static final UriMatcher sUriMatcher;

    private static final int ALBUMS = 1;

    private static final int ALBUM_ID = 2;

    private static final int ARTISTS = 3;

    private static final int ARTIST_ID = 4;

    private static final int ARTIST_IDSTR = 5;

    private static final int TRACKS = 6;

    private static final int TRACK_ID = 7;

    private static final int TAGS = 8;

    private static final int TAG_ID = 9;

    private static final int TAG_IDSTR = 10;

    private static final int REVIEWS = 11;

    private static final int REVIEW_ID = 12;

    private static final int PLAYLISTS = 13;

    private static final int PLAYLIST_ID = 14;

    private static final int USERS = 15;

    private static final int USER_ID = 16;

    private static final int USER_IDSTR = 17;

    private static final int SUGGEST = 18;

    private static final String TABLE_ALBUM = "album";

    private static final String TABLE_ARTIST = "artist";

    private static final String TABLE_TRACK = "track";

    private static final String TABLE_TAG = "tag";

    private static final String TABLE_REVIEW = "review";

    private static final String TABLE_PLAYLIST = "playlist";

    private static final String TABLE_USER = "user";

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "albums", ALBUMS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "albums/#", ALBUM_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "artists", ARTISTS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "artists/#", ARTIST_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "artists/*", ARTIST_IDSTR);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "tracks", TRACKS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "tracks/#", TRACK_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "tags", TAGS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "tags/#", TAG_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "tags/*", TAG_IDSTR);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "reviews", REVIEWS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "reviews/#", REVIEW_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "playlists", PLAYLISTS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "playlists/#", PLAYLIST_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "users", USERS);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "users/#", USER_ID);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, "users/*", USER_IDSTR);
        sUriMatcher.addURI(JamendoContract.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SUGGEST);
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ALBUMS:
                return Albums.CONTENT_TYPE;
            case ALBUM_ID:
                return Albums.CONTENT_ITEM_TYPE;
            case ARTISTS:
                return Artists.CONTENT_TYPE;
            case ARTIST_ID:
            case ARTIST_IDSTR:
                return Artists.CONTENT_ITEM_TYPE;
            case TRACKS:
                return Tracks.CONTENT_TYPE;
            case TRACK_ID:
                return Tracks.CONTENT_ITEM_TYPE;
            case TAGS:
                return Tags.CONTENT_TYPE;
            case TAG_ID:
            case TAG_IDSTR:
                return Tags.CONTENT_ITEM_TYPE;
            case REVIEWS:
                return Reviews.CONTENT_TYPE;
            case REVIEW_ID:
                return Reviews.CONTENT_ITEM_TYPE;
            case PLAYLISTS:
                return Playlists.CONTENT_TYPE;
            case PLAYLIST_ID:
                return Playlists.CONTENT_ITEM_TYPE;
            case USERS:
                return Users.CONTENT_TYPE;
            case USER_ID:
            case USER_IDSTR:
                return Users.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    private static final String join(String[] parts, char separator) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i != 0) {
                buffer.append(separator);
            }
            buffer.append(parts[i]);
        }
        return buffer.toString();
    }

    private static final String join(List<String> parts, char separator) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i != 0) {
                buffer.append(separator);
            }
            buffer.append(parts.get(i));
        }
        return buffer.toString();
    }

    private static final String[] mapColumnNames(String[] input, Map<String, String> projectionMap) {
        String[] output = new String[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        for (int i = 0; i < input.length; i++) {
            if (projectionMap.containsKey(input[i])) {
                output[i] = projectionMap.get(input[i]);
            }
        }
        return output;
    }

    private static String getTable(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ALBUMS:
            case SUGGEST:
            case ALBUM_ID:
                return TABLE_ALBUM;
            case ARTISTS:
            case ARTIST_ID:
            case ARTIST_IDSTR:
                return TABLE_ARTIST;
            case TRACKS:
            case TRACK_ID:
                return TABLE_TRACK;
            case TAGS:
            case TAG_ID:
            case TAG_IDSTR:
                return TABLE_TAG;
            case REVIEWS:
            case REVIEW_ID:
                return TABLE_REVIEW;
            case PLAYLISTS:
            case PLAYLIST_ID:
                return TABLE_PLAYLIST;
            case USERS:
            case USER_ID:
            case USER_IDSTR:
                return TABLE_USER;
            default:
                return null;
        }
    }

    private static int length(String[] array) {
        int sum = 0;
        if (array != null) {
            for (String s : array) {
                sum += s.length();
            }
        }
        return sum;
    }

    private static String setSelectionArgs(String selection, String[] selectionArgs) {
        int capacity = selection.length() + length(selectionArgs);
        StringBuilder builder = new StringBuilder(capacity);
        int start = 0;
        int arg = 0;
        for (;;) {
            int index = selection.indexOf('?', start);
            if (index == -1) {
                int end = selection.length();
                builder.append(selection, start, end);
                break;
            }
            builder.append(selection, start, index);
            String value = Uri.encode(selectionArgs[arg++]);
            builder.append(value);
            start = index + 1;
        }
        return builder.toString();
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        JamendoCache.install(context);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (projection == null) {
            switch (sUriMatcher.match(uri)) {
                case SUGGEST:
                    projection = new String[] {
                            BaseColumns._ID, SearchManager.SUGGEST_COLUMN_ICON_1,
                            SearchManager.SUGGEST_COLUMN_TEXT_1,
                            SearchManager.SUGGEST_COLUMN_TEXT_2,
                            SearchManager.SUGGEST_COLUMN_INTENT_DATA
                    };
                    break;
                default:
                    // TODO: Specify default projections for each query type
                    return null;
            }
        }
        MatrixCursor output = new MatrixCursor(projection);
        Bundle extras = new Bundle();
        try {
            if (selection == null) {
                selection = "";
            }
            selection = setSelectionArgs(selection, selectionArgs);
            Map<String, String> projectionMap = new HashMap<String, String>();
            projectionMap.put(BaseColumns._ID, "id");
            switch (sUriMatcher.match(uri)) {
                case SUGGEST:
                    projectionMap.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, "id");
                    projectionMap.put(SearchManager.SUGGEST_COLUMN_ICON_1, "id");
                    projectionMap.put(SearchManager.SUGGEST_COLUMN_ICON_2, Albums.IMAGE);
                    projectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1, Albums.NAME);
                    projectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_2, Artists.NAME);
                    break;
            }

            String table = getTable(uri);
            Uri.Builder builder = BASE_URI.buildUpon();
            builder.appendEncodedPath(join(mapColumnNames(projection, projectionMap), '+'));
            builder.appendPath(table);
            builder.appendPath("xml");

            {
                QueryBuilder queryBuilder = new QueryBuilder(selection);
                List<String> join = queryBuilder.getQueryParameters(JamendoContract.PARAM_JOIN);
                if (!join.isEmpty()) {
                    builder.appendEncodedPath(join(join, '+'));
                }
                // TODO: Remove "join" parameters that are used here
                // but are not part of the official Jamendo API.
            }

            // Copy the selection verbatim to the query
            if (selection != null) {
                builder.encodedQuery(selection);
            }

            // Append the sort order verbatim to the query
            if (sortOrder != null) {
                builder.appendQueryParameter("order", sortOrder);
            }

            switch (sUriMatcher.match(uri)) {
                case ALBUM_ID:
                case ARTIST_ID:
                case TRACK_ID:
                case TAG_ID:
                case REVIEW_ID:
                case PLAYLIST_ID:
                case USER_ID:
                    long id = ContentUris.parseId(uri);
                    builder.appendQueryParameter("id", String.valueOf(id));
                    break;
            }

            switch (sUriMatcher.match(uri)) {
                case ARTIST_IDSTR:
                case TAG_IDSTR:
                case USER_IDSTR:
                    String idstr = uri.getLastPathSegment();
                    builder.appendQueryParameter("idstr", idstr);
                    break;
            }

            switch (sUriMatcher.match(uri)) {
                case SUGGEST:
                    String query = uri.getLastPathSegment();
                    builder.appendQueryParameter("searchquery", query);
                    break;
            }

            builder.appendQueryParameter("n", String.valueOf(PAGE_SIZE));

            ContentHandler handler = new JamendoContentHandler(output, table, projectionMap);

            long defaultMaxAge = DateUtils.DAY_IN_MILLIS;
            Long maxAge = Long.valueOf(JamendoContract.getMaxAge(uri, defaultMaxAge));
            handler = JamendoCache.capture(handler, maxAge);

            Uri feedUri = builder.build();
            String queryParam = "pn";
            int firstPage = 1;
            int n = FeedUri.getItemCount(uri, PAGE_SIZE);
            FeedLoader.loadPagedFeed(handler, feedUri, queryParam, firstPage, PAGE_SIZE, n, extras);
            return FeedProvider.feedCursor(output, extras);
        } catch (Throwable t) {
            Log.e(TAG, "query failed", t);
            return FeedProvider.errorCursor(output, extras, t);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
