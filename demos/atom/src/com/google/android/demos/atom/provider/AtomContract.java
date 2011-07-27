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

package com.google.android.demos.atom.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Defines the URIs and the content types for the Atom content provider, and
 * provides helper methods for building and analyzing Atom content URIs.
 */
public class AtomContract {

    public static final String AUTHORITY = "com.google.android.demos.atom";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    static final String PATH_FEEDS = "feeds";

    static final String PATH_ENTRIES = "entries";

    /**
     * Column definitions for Atom feed entries.
     * <p>
     * RFC 4287: 4.1.2 The "atom:entry" Element
     * <p>
     * The "atom:entry" Element represents an individual entry, acting as a
     * container for metadata and data associated with the entry. This element
     * can appear as a child of the atom:feed element, or it can appear as the
     * document (i.e., top-level) element of a stand-alone Atom Entry Document.
     * <p>
     * TODO: Add more columns for elements defined in RFC 4287
     */
    interface EntriesColumns extends BaseColumns {

        /**
         * RFC 4287: 4.2.6 The "atom:id" Element
         * <p>
         * The "atom:id" Element conveys a permanent, universally unique
         * identifier for an entry or feed.
         * <p>
         * Type: TEXT
         */
        String ID = "atom_id";

        /**
         * RFC 4287: 4.2.14 The "atom:title" Element
         * <p>
         * The "atom:title" Element is a Text construct that conveys a human-
         * readable title for an entry or feed.
         * <p>
         * Depending on the source, the title may be HTML mark-up. To get a
         * plain-text title, use {@link #TITLE_PLAINTEXT}.
         * <p>
         * Type: TEXT
         */
        String TITLE = "atom_title";

        /**
         * The {@link #TITLE} as plain text.
         * <p>
         * Type: TEXT
         */
        String TITLE_PLAINTEXT = "atom_title_plaintext";

        /**
         * RFC 4287: 4.2.9 The "atom:published" Element
         * <p>
         * The "atom:published" Element is a Date construct indicating an
         * instant in time associated with an event early in the life cycle of
         * the entry.
         * <p>
         * Type: TEXT (RFC 822 format)
         */
        String PUBLISHED = "atom_published";

        /**
         * RFC 4287: 4.2.15 The "atom:updated" Element
         * <p>
         * The "atom:updated" Element is a Date construct indicating the most
         * recent instant in time when an entry or feed was modified in a way
         * the publisher considers significant. Therefore, not all modifications
         * necessarily result in a changed atom:updated value.
         * <p>
         * Type: TEXT (RFC 822 format)
         */
        String UPDATED = "atom_updated";

        /**
         * RFC 4287: 4.2.13 The "atom:summary" Element
         * <p>
         * The "atom:summary" Element is a Text construct that conveys a short
         * summary, abstract, or excerpt of an entry.
         * <p>
         * Type: TEXT
         */
        String SUMMARY = "atom_summary";

        /**
         * RFC 4287: 4.1.3 The "atom:content" Element
         * <p>
         * The "atom:content" Element either contains or links to the content of
         * the entry. The content of atom:content is Language-Sensitive.
         * <p>
         * Type: TEXT
         */
        String CONTENT = "atom_content";

        /**
         * The value of the {@code href} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"}.
         */
        String ALTERNATE_HREF = "atom_alternate_href";

        /**
         * The value of the {@code type} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"}.
         */
        String ALTERNATE_TYPE = "atom_alternate_type";

        /**
         * The value of the {@code length} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"}.
         */
        String ALTERNATE_LENGTH = "atom_alternate_length";

        /**
         * The value of the {@code href} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="related"}.
         */
        String RELATED_HREF = "atom_related_href";

        /**
         * The value of the {@code type} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="related"}.
         */
        String RELATED_TYPE = "atom_related_type";

        /**
         * The value of the {@code length} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="related"}.
         */
        String RELATED_LENGTH = "atom_related_length";

        /**
         * The value of the {@code href} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="enclosure"}.
         */
        String ENCLOSURE_HREF = "atom_enclosure_href";

        /**
         * The value of the {@code type} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="enclosure"}.
         */
        String ENCLOSURE_TYPE = "atom_enclosure_type";

        /**
         * The value of the {@code length} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="enclosure"}.
         */
        String ENCLOSURE_LENGTH = "atom_enclosure_length";

        /**
         * The value of the {@code href} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="via"}.
         */
        String VIA_HREF = "atom_via_href";

        /**
         * The value of the {@code title} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="via"}.
         */
        String VIA_TITLE = "atom_via_title";

        /**
         * RFC 4287: 4.2.11 The "atom:source" Element
         * <p>
         * If an atom:entry is copied from one feed into another feed, then the
         * source atom:feed's metadata (all child elements of atom:feed other
         * than the atom:entry elements) MAY be preserved within the copied
         * entry by adding an atom:source child element, if it is not already
         * present in the entry, and including some or all of the source feed's
         * Metadata elements as the atom:source element's children. Such
         * metadata SHOULD be preserved if the source atom:feed contains any of
         * the child elements atom:author, atom:contributor, atom:rights, or
         * atom:category and those child elements are not present in the source
         * atom:entry.
         * 
         * @see FeedsColumns#ID
         */
        String SOURCE_ID = "atom_source_id";

        /**
         * RFC 4287: 4.2.11 The "atom:source" Element
         * <p>
         * If an atom:entry is copied from one feed into another feed, then the
         * source atom:feed's metadata (all child elements of atom:feed other
         * than the atom:entry elements) MAY be preserved within the copied
         * entry by adding an atom:source child element, if it is not already
         * present in the entry, and including some or all of the source feed's
         * Metadata elements as the atom:source element's children. Such
         * metadata SHOULD be preserved if the source atom:feed contains any of
         * the child elements atom:author, atom:contributor, atom:rights, or
         * atom:category and those child elements are not present in the source
         * atom:entry.
         * 
         * @see FeedsColumns#TITLE
         */
        String SOURCE_TITLE = "atom_source_title";

        /**
         * The value of the {@code href} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"} within the
         * "atom:source" element.
         */
        String SOURCE_ALTERNATE_HREF = "atom_source_alternate_href";

        /**
         * The value of the {@code type} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"} within the
         * "atom:source" element.
         */
        String SOURCE_ALTERNATE_TYPE = "atom_source_alternate_type";

        /**
         * The value of the {@code length} attribute for an arbitrary
         * {@code <link>} with attribute {@code rel="alternate"} within the
         * "atom:source" element.
         */
        String SOURCE_ALTERNATE_LENGTH = "atom_source_alternate_length";
    }

    /**
     * Columns definitions for Atom feed metadata.
     * <p>
     * This interface defines columns that would only appear once in an Atom XML
     * document, not once per {@code entry}. RFC 4287: 4.1.1 The "atom:feed"
     * Element
     * <p>
     * The "atom:feed" element is the document (i.e., top-level) element of an
     * Atom Feed Document, acting as a container for metadata and data
     * associated with the feed. Its element children consist of metadata
     * elements followed by zero or more atom:entry child elements.
     * <p>
     * TODO: Add more columns for elements defined in RFC 4287
     */
    public interface FeedsColumns extends BaseColumns {
        /**
         * RFC 4287: 4.2.6 The "atom:id" Element
         * <p>
         * The "atom:id" element conveys a permanent, universally unique
         * identifier for an entry or feed.
         */
        String ID = "atom_id";

        /**
         * RFC 4287: 4.2.14 The "atom:title" Element
         * <p>
         * The "atom:title" element is a Text construct that conveys a human-
         * readable title for an entry or feed.
         */
        String TITLE = "atom_title";

        /**
         * The {@link #TITLE} as plain text.
         * <p>
         * Type: TEXT
         */
        String TITLE_PLAINTEXT = "atom_title_plaintext";

        /**
         * RFC 4287: 4.2.12 The "atom:subtitle" Element
         * <p>
         * The "atom:subtitle" element is a Text construct that conveys a human-
         * readable description or subtitle for a feed.
         */
        String SUBTITLE = "atom_subtitle";

        /**
         * RFC 4287: 4.2.15 The "atom:updated" Element
         * <p>
         * The "atom:updated" element is a Date construct indicating the most
         * recent instant in time when an entry or feed was modified in a way
         * the publisher considers significant. Therefore, not all modifications
         * necessarily result in a changed atom:updated value.
         */
        String UPDATED = "atom_updated";
    }

    /**
     * Columns definitions for {@code link} elements found within Atom entries.
     * RFC 4287: 4.2.7 The "atom:link" Element
     * <p>
     * The "atom:link" element defines a reference from an entry or feed to a
     * Web resource. This specification assigns no meaning to the content (if
     * any) of this element.
     */
    public interface LinksColumns extends BaseColumns {

        /**
         * RFC 4287: 4.2.7.1 The "href" Attribute
         * <p>
         * The "href" attribute contains the link's IRI. atom:link elements MUST
         * have an href attribute, whose value MUST be a IRI reference
         * [RFC3987].
         */
        String HREF = "atom_href";

        /**
         * RFC 4287: 4.2.7.2 The "rel" Attribute
         * <p>
         * atom:link elements MAY have a "rel" attribute that indicates the link
         * relation type. If the "rel" attribute is not present, the link
         * element MUST be interpreted as if the link relation type is
         * "alternate".
         */
        String REL = "atom_rel";

        /**
         * RFC 4287: 4.2.7.3 The "type" Attribute
         * <p>
         * On the link element, the "type" attribute's value is an advisory
         * media type: it is a hint about the type of the representation that is
         * expected to be returned when the value of the href attribute is
         * dereferenced. Note that the type attribute does not override the
         * actual media type returned with the representation. Link elements MAY
         * have a type attribute, whose value MUST conform to the syntax of a
         * MIME media type [MIMEREG].
         */
        String TYPE = "atom_type";

        /**
         * RFC 4287: 4.2.7.4 The "hreflang" Attribute
         * <p>
         * The "hreflang" attribute's content describes the language of the
         * resource pointed to by the href attribute. When used together with
         * the rel="alternate", it implies a translated version of the entry.
         * Link elements MAY have an hreflang attribute, whose value MUST be a
         * language tag [RFC3066].
         */
        String HREFLANG = "atom_hreflang";

        /**
         * RFC 4287: 4.2.7.5 The "title" Attribute
         * <p>
         * The "title" attribute conveys human-readable information about the
         * link.  The content of the "title" attribute is Language-Sensitive.
         * Entities such as "&amp;" and "&lt;" represent their corresponding
         * characters ("&" and "<", respectively), not markup.  Link elements
         * MAY have a title attribute.
         */
        String TITLE = "atom_title";

        /**
         * RFC 4287: 4.2.7.6 The "length" Attribute
         * <p>
         * The "length" attribute indicates an advisory length of the linked
         * content in octets; it is a hint about the content length of the
         * representation returned when the IRI in the href attribute is mapped
         * to a URI and dereferenced. Note that the length attribute does not
         * override the actual content length of the representation as reported
         * by the underlying protocol. Link elements MAY have a length
         * attribute.
         */
        String LENGTH = "atom_length";
    }

    public static class Feeds implements FeedsColumns {

        private static String normalize(String feed) {
            // See http://en.wikipedia.org/wiki/Feed_protocol
            if (feed.startsWith("feed://")) {
                // Replace feed: with http:
                //
                // Example: feed://example.com/rss.xml
                return "http://" + feed.substring("feed://".length());
            } else if (feed.startsWith("feed:")) {
                // Remove feed: if protocol is specified
                //
                // Example: feed:https://example.com/rss.xml
                return feed.substring("feed:".length());
            } else {
                // The feed is already http: or https:
                return feed;
            }
        }

        public static boolean isValidFeedUrl(String urlString) {
            urlString = normalize(urlString);
            try {
                URL url = new URL(urlString);
                String protocol = url.getProtocol();
                return "http".equals(protocol) || "https".equals(protocol);
            } catch (MalformedURLException e) {
                return false;
            }
        }
        private Feeds() {
        }
    }

    public static class Entries implements EntriesColumns {

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.demos.atom.entry";

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.demos.atom.entry";

        private Entries() {
        }

        public static String getFeedUrl(Uri uri) {
            String feed = uri.getPathSegments().get(1);
            return Feeds.normalize(feed);
        }

        public static String getEntryId(Uri uri) {
            return uri.getLastPathSegment();
        }

        public static Uri itemUri(String feedUrl, String entryId) {
            Uri.Builder uri = AUTHORITY_URI.buildUpon();
            uri.appendPath(PATH_FEEDS);
            uri.appendPath(feedUrl);
            uri.appendPath(PATH_ENTRIES);
            uri.appendPath(entryId);
            return uri.build();
        }

        public static Uri contentUri(String feedUrl) {
            Uri.Builder uri = AUTHORITY_URI.buildUpon();
            uri.appendPath(PATH_FEEDS);
            uri.appendPath(feedUrl);
            uri.appendPath(PATH_ENTRIES);
            return uri.build();
        }
    }

    public static class Links implements LinksColumns {
        private Links() {
        }
    }

    private AtomContract() {
    }
}
