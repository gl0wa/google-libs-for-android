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

package com.google.android.demos.atom.content;

import com.google.android.demos.atom.provider.AtomContract.Entries;
import com.google.android.demos.atom.provider.AtomContract.FeedsColumns;
import com.google.android.demos.atom.provider.AtomContract.LinksColumns;
import com.google.android.feeds.content.FeedLoader;
import com.google.android.feeds.net.XmlContentHandler;

import org.xml.sax.Attributes;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.Html;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts XML events to rows of {@link Entries}.
 */
public class AtomContentHandler extends XmlContentHandler {

    /**
     * Binds text inside XML tags to cursor columns.
     */
    public interface ColumnBinder {
        /**
         * Maps a column name and value to zero or more entries in a row.
         * 
         * @param row the output row.
         * @param columnName the name of the column passed to
         *            {@link AtomContentHandler#column(Element, String, String, String)}
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
         *            {@link AtomContentHandler#extra(Element, String, String, String)}
         * @param value the raw text value.
         * @return {@code true} if the extra was bound, {@code false} to fall
         *         back to the default binding.
         */
        boolean setExtraValue(Bundle extras, String key, String value);
    }

    /**
     * The XML namespace for Atom elements.
     */
    public static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";

    private static final String[] LINKS_PROJECTION = {
            LinksColumns._ID, LinksColumns.HREF, LinksColumns.REL, LinksColumns.TYPE,
            LinksColumns.HREFLANG, LinksColumns.TITLE, LinksColumns.LENGTH
    };

    private static final String[] LINK_ATTRIBUTES = {
            null, "href", "rel", "type", "hreflang", "title", "length"
    };

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
    private static boolean isPlainText(String source) {
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

    private final Element mEntryElement;

    private final RootElement mFeedElement;

    /**
     * The output cursor.
     */
    protected final MatrixCursor mOutput;

    /**
     * The output cursor extras.
     */
    protected final Bundle mExtras;

    /**
     * Column values for the current row.
     */
    protected final ContentValues mRow;
    
    private int mRowCount;

    private final Map<Element, List<String>> mExtraMap;

    private final Map<Element, List<String>> mColumnMap;

    private ColumnBinder mColumnBinder;

    private ExtraBinder mExtraBinder;

    private String mFilter;

    private String mId;

    /**
     * Constructor.
     * 
     * @param output the output buffer.
     * @param extras a {@link Bundle} to hold values that should be returned by
     *            {@link Cursor#getExtras()}.
     * @throws NullPointerException if either argument is {@code null}.
     */
    public AtomContentHandler(MatrixCursor output, Bundle extras) {
        if (output == null) {
            throw new NullPointerException();
        }
        if (extras == null) {
            throw new NullPointerException();
        }
        mOutput = output;
        mExtras = extras;

        mRow = new ContentValues();
        mExtraMap = new HashMap<Element, List<String>>();
        mColumnMap = new HashMap<Element, List<String>>();

        RootElement feed = mFeedElement = new RootElement(ATOM_NAMESPACE, "feed");
        feed.setElementListener(new DocumentListener());
        extra(feed, ATOM_NAMESPACE, "id", FeedsColumns.ID);
        extra(feed, ATOM_NAMESPACE, "updated", FeedsColumns.UPDATED);
        extra(feed, ATOM_NAMESPACE, "title", FeedsColumns.TITLE);
        extra(feed, ATOM_NAMESPACE, "title", FeedsColumns.TITLE_PLAINTEXT);
        extra(feed, ATOM_NAMESPACE, "subtitle", FeedsColumns.SUBTITLE);

        Element entry = mEntryElement = feed.getChild(ATOM_NAMESPACE, "entry");
        entry.setElementListener(new ElementListener() {
            /**
             * {@inheritDoc}
             */
            public void start(Attributes attributes) {
                startEntry(attributes);
            }

            /**
             * {@inheritDoc}
             */
            public void end() {
                if (endEntry()) {
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
        column(entry, ATOM_NAMESPACE, "id", Entries._ID);
        column(entry, ATOM_NAMESPACE, "id", Entries.ID);
        column(entry, ATOM_NAMESPACE, "title", Entries.TITLE);
        column(entry, ATOM_NAMESPACE, "title", Entries.TITLE_PLAINTEXT);
        column(entry, ATOM_NAMESPACE, "published", Entries.PUBLISHED);
        column(entry, ATOM_NAMESPACE, "updated", Entries.UPDATED);
        column(entry, ATOM_NAMESPACE, "summary", Entries.SUMMARY);
        column(entry, ATOM_NAMESPACE, "content", Entries.CONTENT);
        links(entry, "atom_");

        Element source = entry.getChild(ATOM_NAMESPACE, "source");
        source.setElementListener(new ElementListener() {
            /**
             * {@inheritDoc}
             */
            public void start(Attributes attributes) {
                startSource(attributes);
            }

            /**
             * {@inheritDoc}
             */
            public void end() {
                endSource();
            }
        });
        column(source, ATOM_NAMESPACE, "id", Entries.SOURCE_ID);
        column(source, ATOM_NAMESPACE, "title", Entries.SOURCE_TITLE);
        links(source, "atom_source_");
    }

    /**
     * Maps link attributes to column names.
     * 
     * @param parent the parent element containing the link.
     * @param prefix a prefix for the column name.
     */
    public void links(Element parent, String prefix) {
        Element link = parent.getChild(ATOM_NAMESPACE, "link");
        link.setStartElementListener(new LinkListener(prefix));
    }

    /**
     * Configures the handler to filter-out all rows that do not have this ID.
     */
    public final void setFilter(String id) {
        mFilter = id;
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
        } else if (columnName.equals(Entries.ID)) {
            // The ID is required for filtering.
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
        if (columnName.equals(Entries.ID)) {
            mId = value;
        }
        if (mColumnBinder == null || !mColumnBinder.setColumnValue(mRow, columnName, value)) {
            if (columnName.equals(Entries.TITLE_PLAINTEXT)) {
                String plaintext = convertHtmlToPlainText(value);
                mRow.put(columnName, plaintext);
            } else if (columnName.equals(Entries._ID)) {
                long longId = Math.abs(value.hashCode());
                mRow.put(columnName, Long.valueOf(longId));
            } else {
                mRow.put(columnName, value);
            }
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
            if (key.equals(FeedsColumns.TITLE_PLAINTEXT)) {
                String text = convertHtmlToPlainText(value);
                mExtras.putString(key, text);
            } else {
                mExtras.putString(key, value);
            }
        }
    }

    /**
     * Maps a text element to a column if {@link #isColumnRequired(String)}
     * returns {@code true}.
     * 
     * @param element the parent tag
     * @param uri the namespace of the tag containing the text (may be an empty
     *            string).
     * @param localName the name of the tag containing the text.
     * @param columnName the name of the column to set.
     * @throws NullPointerException if any argument is {@code null}
     */
    public void column(Element element, String uri, String localName, String columnName) {
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
     * {@link Entries#TITLE} to {@link android.provider.LiveFolders#NAME}.
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

    /**
     * Returns the {@link Element} for the {@code entry} tag.
     */
    public Element getEntryElement() {
        return mEntryElement;
    }

    /**
     * Returns the {@link Element} for the {@code feed} tag.
     */
    public RootElement getFeedElement() {
        return mFeedElement;
    }

    /**
     * Called when a new entry is started.
     * <p>
     * This method can be overridden to set columns from attribute values.
     * <p>
     * Overriding methods should call the superclass' implementation first.
     * 
     * @param attributes the tag attributes.
     */
    protected void startEntry(Attributes attributes) {
        mRow.clear();

        // Set default values
        mRow.put(Entries.SUMMARY, "");
        mRow.put(Entries.CONTENT, "");
    }

    /**
     * Called after an entry has been parsed completely.
     * <p>
     * Overriding classes should call and return super unless they are
     * explicitly returning {@code false}.
     * 
     * @return {@code true} if this entry should be included, {@code false} if
     *         it should not be included (for instance, in the case where it was
     *         deleted but the operation is not yet reflected in the cache
     *         data).
     */
    protected boolean endEntry() {
        return mFilter == null || mFilter.equals(mId);
    }

    /**
     * Called when an entry source is started.
     * 
     * @param attributes the {@code <source>} attributes.
     */
    protected void startSource(Attributes attributes) {
    }

    /**
     * Called when an entry source is finished.
     */
    protected void endSource() {
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        parse(connection, mFeedElement.getContentHandler());
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

    private class LinkListener implements StartElementListener {

        private final String mPrefix;

        private final ContentValues mSubRow;

        public LinkListener(String prefix) {
            mPrefix = prefix;
            mSubRow = new ContentValues();
        }

        /**
         * {@inheritDoc}
         */
        public void start(Attributes attributes) {
            // TODO: Consider using a lookup table to avoid string
            // concatenations.
            mSubRow.clear();
            String rel = attributes.getValue("rel");
            if (rel != null) {
                // assert: LINKS_PROJECTION.length == LINK_ATTRIBUTES.length
                for (int columnIndex = 0; columnIndex < LINKS_PROJECTION.length; columnIndex++) {
                    String columnName = LINKS_PROJECTION[columnIndex];
                    String attribute = LINK_ATTRIBUTES[columnIndex];
                    if (columnName.equals(LinksColumns._ID)) {
                        // Generate ID from href attribute
                        String href = attributes.getValue("href");
                        long longId = 0L;
                        if (href != null) {
                            longId = href.hashCode();
                        }
                        longId = Math.abs(longId);
                        mSubRow.put(columnName, Long.valueOf(longId));
                    } else {
                        String value = attributes.getValue(attribute);

                        // Set convenience column
                        if (!attribute.equals(LinksColumns.REL)) {
                            String convenienceColumnName = mPrefix + rel + "_" + attribute;
                            mRow.put(convenienceColumnName, value);
                        } else {
                            // Skip redundant columns where the value is
                            // already in the name
                            // (for example, "atom_alternate_rel" =
                            // "alternate").
                        }

                        // Set columns for sub-cursors like "alternate" or
                        // "enclosure"
                        mSubRow.put(columnName, value);
                    }
                }
            }
        }
    }
}
