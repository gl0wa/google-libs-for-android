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

package com.google.android.demos.rss.content;

import com.google.android.demos.rss.provider.RssContract.Channels;
import com.google.android.demos.rss.provider.RssContract.Items;
import com.google.android.feeds.FeedLoader;
import com.google.android.feeds.XmlContentHandler;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.Html;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link ContentHandler} for RSS feeds with built-in support for common RSS
 * extensions.
 */
public class RssContentHandler extends XmlContentHandler {

    /**
     * Binds text inside XML tags to cursor columns.
     */
    public interface ColumnBinder {
        /**
         * Maps a column name and value to zero or more entries in a row.
         * 
         * @param row the output row.
         * @param columnName the name of the column passed to
         *            {@link RssContentHandler#column(Element, String, String, String)}
         * @param value the raw text value.
         * @return {@code true} if the column was bound, {@code false} to fall
         *         back to the default binding.
         */
        boolean setColumnValue(ContentValues row, String columnName, String value);
    }

    /**
     * Binds text inside XML tags to cursor extras.
     * 
     * @see Cursor#getExtras()
     */
    public interface ExtraBinder {
        /**
         * Maps a extra key and value to zero or more entries in a
         * {@link Bundle}.
         * 
         * @param extras the output {@link Bundle}.
         * @param key the key passed to
         *            {@link RssContentHandler#extra(Element, String, String, String)}
         * @param value the raw text value.
         * @return {@code true} if the extra was bound, {@code false} to fall
         *         back to the default binding.
         */
        boolean setExtraValue(Bundle extras, String key, String value);
    }

    private static final String RSS_NAMESPACE = "";

    private static final String MRSS_NAMESPACE = "http://search.yahoo.com/mrss/";

    /**
     * The plain-text version of an HTML snippet is identical to the original
     * HTML snippet unless the snippet contains any of the characters in this
     * {@link String}.
     */
    private static final String HTML_CHARACTERS = "&<>";

    /**
     * Returns {@code true} if the given HTML mark-up would remain the same
     * after HTML-to-text conversion, {@code false} otherwise.
     * <p>
     * Use this method to avoid costly HTML parsing when it is not necessary.
     */
    static boolean isPlainText(String source) {
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            
            // Check for special HTML characters
            if (HTML_CHARACTERS.indexOf(c) != -1) {
                return false;
            }

            // Check for consecutive whitespace that would need to be collapsed
            if (i > 0 && Character.isWhitespace(c) && Character.isWhitespace(source.charAt(i - 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts HTML to plain text.
     * 
     * @param source HTML markup.
     * @return plain text.
     */
    protected static String convertHtmlToPlainText(String source) {
        return isPlainText(source) ? source : Html.fromHtml(source).toString();
    }

    private final RootElement mRssElement;

    private final Element mChannelElement;

    private final Element mItemElement;

    protected final MatrixCursor mOutput;

    protected final Bundle mExtras;

    protected final ContentValues mRow;
    
    private int mRowCount;

    private final Map<Element, List<String>> mExtraMap;

    private final Map<Element, List<String>> mColumnMap;

    private ColumnBinder mColumnBinder;

    private ExtraBinder mExtraBinder;

    private String mGuid;

    private String mFilter;

    /**
     * Constructor.
     * 
     * @param out the output buffer.
     * @param extras a {@link Bundle} to hold values that should be returned by
     *            {@link Cursor#getExtras()}.
     * @throws NullPointerException if either argument is {@code null}
     */
    public RssContentHandler(MatrixCursor out, Bundle extras) {
        if (out == null) {
            throw new NullPointerException();
        }
        if (extras == null) {
            throw new NullPointerException();
        }
        mOutput = out;
        mExtras = extras;
        mRow = new ContentValues();
        mExtraMap = new HashMap<Element, List<String>>();
        mColumnMap = new HashMap<Element, List<String>>();
        mRssElement = new RootElement(RSS_NAMESPACE, "rss");
        mChannelElement = mRssElement.getChild(RSS_NAMESPACE, "channel");
        mChannelElement.setElementListener(new DocumentListener());
        mItemElement = mChannelElement.getChild(RSS_NAMESPACE, "item");
        mItemElement.setElementListener(new ElementListener() {
            /**
             * {@inheritDoc}
             */
            public void start(Attributes attributes) {
                startItem(attributes);
            }

            /**
             * {@inheritDoc}
             */
            public void end() {
                if (endItem()) {
                    MatrixCursor.RowBuilder builder = mOutput.newRow();
                    int columnCount = mOutput.getColumnCount();
                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        String columnName = mOutput.getColumnName(columnIndex);
                        builder.add(mRow.get(columnName));
                    }
                }
                mRowCount++;
            }
        });
        extra(mChannelElement, RSS_NAMESPACE, "title", Channels.TITLE);
        extra(mChannelElement, RSS_NAMESPACE, "title", Channels.TITLE_PLAINTEXT);
        extra(mChannelElement, RSS_NAMESPACE, "language", Channels.LANGUAGE);
        extra(mChannelElement, RSS_NAMESPACE, "description", Channels.DESCRIPTION);
        extra(mChannelElement, RSS_NAMESPACE, "link", Channels.LINK);

        column(mItemElement, RSS_NAMESPACE, "title", Items.TITLE);
        column(mItemElement, RSS_NAMESPACE, "title", Items.TITLE_PLAINTEXT);
        column(mItemElement, RSS_NAMESPACE, "description", Items.DESCRIPTION);
        column(mItemElement, RSS_NAMESPACE, "link", Items.LINK);
        column(mItemElement, RSS_NAMESPACE, "guid", Items._ID);
        column(mItemElement, RSS_NAMESPACE, "guid", Items.GUID);

        column(mItemElement, MRSS_NAMESPACE, "thumbnail", Items.THUMBNAIL);
    }

    /**
     * Called when a new RSS item is started.
     * 
     * @param attributes the item attributes.
     */
    protected void startItem(Attributes attributes) {
        mRow.clear();
        mGuid = null;
    }

    /**
     * Called when an RSS item is ended.
     * 
     * @return {@code true} if the item should be added as a row to the output,
     *         {@code false} if it should not.
     */
    protected boolean endItem() {
        return mFilter == null || mFilter.equals(mGuid);
    }

    /**
     * Returns the {@link Element} for the {@code channel} tag.
     */
    public Element getChannelElement() {
        return mChannelElement;
    }

    /**
     * Returns the {@link Element} for the {@code item} tag.
     */
    public Element getItemElement() {
        return mItemElement;
    }

    /**
     * Maps the body of an element to a {@link String} extra.
     * 
     * @param element the parent tag
     * @param uri the namespace of the tag containing the text (may be an empty
     *            string).
     * @param localName the name of the tag containing the text.
     * @param key the extra key.
     * @throws NullPointerException if any argument is {@code null}
     */
    public void extra(Element element, String uri, String localName, String key) {
        if (element == null) {
            throw new NullPointerException();
        }
        if (uri == null) {
            throw new NullPointerException();
        }
        if (localName == null) {
            throw new NullPointerException();
        }
        if (key == null) {
            throw new NullPointerException();
        }
        Element child = element.getChild(uri, localName);
        if (mExtraMap.containsKey(child)) {
            List<String> extras = mExtraMap.get(child);
            extras.add(key);
        } else {
            child.setEndTextElementListener(new ExtraListener(child));
            List<String> extras = new ArrayList<String>(4);
            extras.add(key);
            mExtraMap.put(child, extras);
        }
    }

    /**
     * Called to set an extra.
     * <p>
     * This method can be overridden to convert the value to a different type or
     * to apply any other kind of post-processing.
     * 
     * @param key the extra key.
     * @param value the extra value.
     */
    protected void handleExtra(String key, String value) {
        if (mExtraBinder == null || !mExtraBinder.setExtraValue(mExtras, key, value)) {
            if (key.equals(Channels.TITLE_PLAINTEXT)) {
                value = value.trim();
                String title = convertHtmlToPlainText(value);
                mExtras.putString(key, title);
            } else {
                mExtras.putString(key, value);
            }
        }
    }

    /**
     * Configures the handler to filter-out all rows that do not have this GUID.
     */
    public final void setFilter(String guid) {
        mFilter = guid;
    }

    /**
     * Returns {@code true} if a column is required even when it is not in the
     * projection, or {@code false} if the column is not required if it is not
     * in the projection.
     * <p>
     * If a column is not required, it will not be parsed and
     * {@link #handleColumn(String, String)} will not be called for the named
     * column.
     */
    protected boolean isColumnRequired(String columnName) {
        if (mOutput.getColumnIndex(columnName) != -1) {
            return true;
        } else if (columnName.equals(Items.GUID)) {
            // The GUID is required for filtering.
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called to set a column value.
     * 
     * @param columnName the column name.
     * @param value the column value (may be {@code null}).
     * @throws NullPointerException if the column name is {@code null}.
     */
    protected void handleColumn(String columnName, String value) {
        if (columnName.equals(Items.GUID)) {
            mGuid = value;
        }

        if (mColumnBinder == null || !mColumnBinder.setColumnValue(mRow, columnName, value)) {
            if (columnName.equals(Items._ID)) {
                long longId = Math.abs(value.hashCode());
                mRow.put(columnName, Long.valueOf(longId));
            } else if (columnName.equals(Items.TITLE_PLAINTEXT)) {
                value = value.trim();
                String title = convertHtmlToPlainText(value);
                mRow.put(columnName, title);
            } else {
                mRow.put(columnName, value);
            }
        }
    }

    /**
     * Maps a text element to a column (if it exists in the projection).
     * 
     * @param element the parent tag
     * @param uri the namespace of the tag containing the text.
     * @param localName the name of the tag containing the text.
     * @param columnName the output column name.
     */
    public void column(Element element, String uri, String localName, final String columnName) {
        if (element == null) {
            throw new NullPointerException();
        }
        if (uri == null) {
            throw new NullPointerException();
        }
        if (localName == null) {
            throw new NullPointerException();
        }
        if (columnName == null) {
            throw new NullPointerException();
        }
        if (isColumnRequired(columnName)) {
            Element child = element.getChild(uri, localName);
            if (mColumnMap.containsKey(child)) {
                List<String> columnNames = mColumnMap.get(child);
                if (!columnNames.contains(columnName)) {
                    columnNames.add(columnName);
                }
            } else {
                child.setEndTextElementListener(new ColumnListener(child));
                List<String> columnNames = new ArrayList<String>(4);
                columnNames.add(columnName);
                mColumnMap.put(child, columnNames);
            }
        }
    }

    /**
     * Returns the binder assigned with {@link #setColumnBinder(ColumnBinder)}
     * or {@code null}.
     */
    public ColumnBinder getColumnBinder() {
        return mColumnBinder;
    }

    /**
     * Sets the binder used to bind the text inside XML tags to cursor columns.
     * <p>
     * Register a {@link ColumnBinder} to override the default column mapping.
     * For example, a {@link ColumnBinder} for a live folder might map
     * {@link Items#TITLE} to {@link android.provider.LiveFolders#NAME}.
     * 
     * @param columnBinder the binder to use, or {@code null}.
     * @see #column(Element, String, String, String)
     */
    public void setColumnBinder(ColumnBinder columnBinder) {
        mColumnBinder = columnBinder;
    }

    /**
     * Returns the binder assigned with {@link #setExtraBinder(ExtraBinder)} or
     * {@code null}.
     */
    public ExtraBinder getExtraBinder() {
        return mExtraBinder;
    }

    /**
     * Sets the binder used to bind text inside XML tags to cursor extras.
     * 
     * @param extraBinder the binder to use, or {@code null}.
     * @see #extra(Element, String, String, String)
     */
    public void setExtraBinder(ExtraBinder extraBinder) {
        mExtraBinder = extraBinder;
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        parse(connection, mRssElement.getContentHandler());
        return FeedLoader.documentInfo(mRowCount);
    }

    private class DocumentListener implements ElementListener {
        /**
         * {@inheritDoc}
         */
        public void start(Attributes attributes) {
            mRowCount = 0;
        }

        /**
         * {@inheritDoc}
         */
        public void end() {
        }
    }

    private class ExtraListener implements EndTextElementListener {
        private final Element mElement;

        public ExtraListener(Element element) {
            mElement = element;
        }

        /**
         * {@inheritDoc}
         */
        public void end(String value) {
            List<String> keys = mExtraMap.get(mElement);
            for (String key : keys) {
                handleExtra(key, value);
            }
        }
    }

    private class ColumnListener implements EndTextElementListener {
        private final Element mElement;

        public ColumnListener(Element element) {
            mElement = element;
        }

        /**
         * {@inheritDoc}
         */
        public void end(String value) {
            List<String> columnNames = mColumnMap.get(mElement);
            for (String columnName : columnNames) {
                handleColumn(columnName, value);
            }
        }
    }
}
