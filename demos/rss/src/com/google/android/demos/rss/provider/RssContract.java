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

package com.google.android.demos.rss.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class RssContract {

    public static final String AUTHORITY = "com.google.android.demos.rss";

    static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    static final String PATH_CHANNELS = "channels";

    static final String PATH_LIVE_FOLDER = "live-folder";

    static final String PATH_ITEMS = "items";

    /**
     * Additional columns for Media RSS (MRSS) extension.
     * <p>
     * Specification: <a href="http://video.search.yahoo.com/mrss">
     * http://video.search.yahoo.com/mrss</a>
     * <p>
     * TODO: Add more column definitions
     */
    interface MediaItemsColumns {
        /**
         * Column definition for {@code <media:thumbnail>}
         * <p>
         * Allows particular images to be used as representative images for a
         * media object.
         */
        String THUMBNAIL = "mrss_thumbnail";
    }

    /**
     * Column defitions for RSS channels.
     * <p>
     * Specification: <a href="http://www.rssboard.org/rss-specification">
     * http://www.rssboard.org/rss-specification</a>
     * <p>
     * TODO: Add column definitions for <a href=
     * "http://www.rssboard.org/rss-specification#optionalChannelElements"
     * >optional elements</a>.
     */
    interface ChannelsColumns {
        /**
         * The name of the channel. It's how people refer to your service. If
         * you have an HTML website that contains the same information as your
         * RSS file, the title of your channel should be the same as the title
         * of your website.
         */
        String TITLE = "rss_title";
        
        /**
         * The title of the item.
         */
        String TITLE_PLAINTEXT = "rss_title_plaintext";

        /**
         * Phrase or sentence describing the channel.
         */
        String DESCRIPTION = "rss_description";

        /**
         * The URL to the HTML website corresponding to the channel
         */
        String LINK = "rss_link";

        /**
         * The language the channel is written in.
         */
        String LANGUAGE = "rss_language";
    }

    /**
     * Column defitions for RSS items.
     * <p>
     * Specification: <a href="http://www.rssboard.org/rss-specification">
     * http://www.rssboard.org/rss-specification</a>
     * <p>
     * TODO: Add column definitions for <a
     * href="http://www.rssboard.org/rss-specification#hrelementsOfLtitemgt">
     * missing item attributes</a>
     */
    public interface ItemsColumns {
        /**
         * The title of the item.
         */
        String TITLE = "rss_title";
        
        /**
         * The title of the item.
         */
        String TITLE_PLAINTEXT = "rss_title_plaintext";

        /**
         * The URL of the item.
         */
        String LINK = "rss_link";

        /**
         * The item synopsis.
         */
        String DESCRIPTION = "rss_description";

        /**
         * Indicates when the item was published.
         */
        String PUBDATE = "rss_pubDate";

        /**
         * A string that uniquely identifies the item.
         */
        String GUID = "rss_guid";
    }

    public static class Channels implements BaseColumns, ChannelsColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.rss.item";

        private Channels() {
        }
    }

    public static class Items implements BaseColumns, ItemsColumns, MediaItemsColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.rss.item";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.rss.item";

        public static Uri contentUri(String channelUrl) {
            Uri.Builder uri = AUTHORITY_URI.buildUpon();
            uri.appendPath(PATH_CHANNELS);
            uri.appendPath(channelUrl);
            uri.appendPath(PATH_ITEMS);
            return uri.build();
        }

        public static Uri itemUri(String channelUrl, String itemGuid) {
            Uri.Builder uri = AUTHORITY_URI.buildUpon();
            uri.appendPath(PATH_CHANNELS);
            uri.appendPath(channelUrl);
            uri.appendPath(PATH_ITEMS);
            uri.appendPath(itemGuid);
            return uri.build();
        }

        public static Uri liveFolderUri(String channelUrl) {
            Uri.Builder uri = AUTHORITY_URI.buildUpon();
            uri.appendPath(PATH_CHANNELS);
            uri.appendPath(channelUrl);
            uri.appendPath(PATH_LIVE_FOLDER);
            return uri.build();
        }

        public static String getChannelUrl(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getItemGuid(Uri uri) {
            return uri.getLastPathSegment();
        }

        private Items() {
        }
    }

    private RssContract() {
    }
}
