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

package com.google.android.demos.rss.content;

import android.content.ContentValues;
import android.provider.LiveFolders;
import android.sax.Element;
import android.text.Html;

/**
 * Binds text inside XML tags of an RSS feed to {@link LiveFolders} columns.
 */
public class LiveFolderBinder implements RssContentHandler.ColumnBinder {

    private static final String RSS_NAMESPACE = "";

    /**
     * A projection containing all the {@link LiveFolders} columns supported by
     * this {@link RssContentHandler.ColumnBinder}.
     */
    public static final String[] PROJECTION = {
            LiveFolders._ID, LiveFolders.NAME, LiveFolders.DESCRIPTION, LiveFolders.INTENT
    };

    /**
     * Maps XML elements to {@link LiveFolders} columns supported by this
     * {@link RssContentHandler.ColumnBinder}.
     */
    public static final void bindColumns(RssContentHandler handler) {
        Element item = handler.getItemElement();
        handler.column(item, RSS_NAMESPACE, "guid", LiveFolders._ID);
        handler.column(item, RSS_NAMESPACE, "title", LiveFolders.NAME);
        handler.column(item, RSS_NAMESPACE, "description", LiveFolders.DESCRIPTION);
        handler.column(item, RSS_NAMESPACE, "link", LiveFolders.INTENT);
    }

    /**
     * Truncates long text to conserve memory.
     */
    private static String truncated(String source) {
        int length = source.length();
        int limit = 100;
        if (length < limit) {
            return source;
        } else {
            return source.substring(0, limit);
        }
    }

    /**
     * Strips HTML tags from the given markup.
     */
    private static String strip(String source) {
        return Html.fromHtml(source).toString();
    }

    /**
     * Collapses whitespace, replacing newlines with spaces so that all of the
     * text appears on one line.
     */
    private static String collapse(String source) {
        return source.replaceAll("\\s+", " ");
    }

    /**
     * {@inheritDoc}
     */
    public boolean setColumnValue(ContentValues row, String columnName, String value) {
        if (columnName.equals(LiveFolders._ID)) {
            // Generate a positive integer ID from the text GUID
            long id = Math.abs(value.hashCode());
            row.put(columnName, Long.valueOf(id));
            return true;
        } else if (columnName.equals(LiveFolders.NAME)
                || columnName.equals(LiveFolders.DESCRIPTION)) {
            // Truncate text, strip HTML, and collapse whitespace.
            value = collapse(strip(truncated(value)));
            row.put(columnName, value);
            return true;
        } else {
            return false;
        }
    }

}
