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

import com.google.android.demos.jamendo.R;
import com.google.android.demos.jamendo.provider.JamendoContract.Albums;
import com.google.android.feeds.FeedLoader;
import com.google.android.feeds.XmlContentHandler;

import org.xml.sax.Attributes;

import android.app.SearchManager;
import android.content.ContentValues;
import android.database.MatrixCursor;
import android.net.Uri;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;

import java.io.IOException;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link XmlContentHandler} for Jamendo API responses.
 */
class JamendoContentHandler extends XmlContentHandler {

    private static final String NAMESPACE = "";

    private final RootElement mRoot;

    private final MatrixCursor mOutput;

    private final Map<String, String> mProjectionMap;

    private final ContentValues mRow;
    
    private int mRowCount;

    public JamendoContentHandler(MatrixCursor out, String table, Map<String, String> projectionMap) {
        mOutput = out;
        mProjectionMap = projectionMap;
        mRow = new ContentValues();

        mRoot = new RootElement(NAMESPACE, "data");
        mRoot.setElementListener(new DocumentListener());

        Element row = mRoot.getChild(NAMESPACE, table);
        row.setElementListener(new RowListener());

        // Build a set of unique element names
        String[] projection = out.getColumnNames();
        Set<String> elementNames = new HashSet<String>(projection.length);
        for (String columnName : projection) {
            String elementName = getElementName(columnName);
            elementNames.add(elementName);
        }

        // Add a listener for each unique element
        for (String elementName : elementNames) {
            Element column = row.getChild(NAMESPACE, elementName);
            column.setEndTextElementListener(new ColumnListener(elementName));
        }
    }

    private String getElementName(String columnName) {
        String elementName = mProjectionMap.get(columnName);
        return elementName != null ? elementName : columnName;
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

    private class RowListener implements ElementListener {
        /**
         * {@inheritDoc}
         */
        public void start(Attributes attributes) {
            mRow.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void end() {
            MatrixCursor.RowBuilder builder = mOutput.newRow();
            for (String columnName : mOutput.getColumnNames()) {
                String elementName = getElementName(columnName);
                String value = mRow.getAsString(elementName);
                if (columnName.equals(SearchManager.SUGGEST_COLUMN_INTENT_DATA)) {
                    builder.add(Uri.withAppendedPath(Albums.CONTENT_URI, value));
                } else if (columnName.equals(SearchManager.SUGGEST_COLUMN_ICON_1)) {
                    builder.add(R.drawable.jamendo_album);
                } else {
                    builder.add(value);
                }
            }
            mRowCount++;
        }
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        parse(connection, mRoot.getContentHandler());
        return FeedLoader.documentInfo(mRowCount);
    }

    private class ColumnListener implements EndTextElementListener {

        private final String mElementName;

        public ColumnListener(String elementName) {
            mElementName = elementName;
        }

        /**
         * {@inheritDoc}
         */
        public void end(String body) {
            // MatrixCursor will handle convert types automatically
            mRow.put(mElementName, body);
        }
    }
}
