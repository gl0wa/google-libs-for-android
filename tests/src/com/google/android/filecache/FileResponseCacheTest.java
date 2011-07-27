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

package com.google.android.filecache;

import android.app.Instrumentation;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Tests for {@link FileResponseCache}.
 *
 * TODO: Move helper classes to separate files
 */
@LargeTest
public class FileResponseCacheTest extends InstrumentationTestCase {

    @SuppressWarnings("unchecked")
    private static <T> T getContent(ContentHandler handler, URLConnection connection) throws IOException {
        HttpURLConnection http = (HttpURLConnection) connection;
        try {
            return (T) handler.getContent(http);
        } finally {
            http.disconnect();
        }
    }
    
    private static <T> T getContent(ContentHandler handler, URL url) throws IOException {
        return getContent(handler, url.openConnection());
    }

    private TestResponseCache mResponseCache;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResponseCache = new TestResponseCache();
        ResponseCache.setDefault(mResponseCache);
    }

    @Override
    protected void tearDown() throws Exception {
        ResponseCache.setDefault(null);
        super.tearDown();
    }
    
    public void testGetResponseCode() throws IOException {
        AssertResponseCodeContentHandler assertResponseCode;
        URL url = new URL("http://www.google.com/");
        File file = createTempFile();
        ContentHandler handler = TestResponseCache.sink();
        handler = assertResponseCode = new AssertResponseCodeContentHandler(handler);
        assertResponseCode.setExpected(HttpURLConnection.HTTP_OK, "OK");
        handler = TestResponseCache.capture(handler, file);
        mResponseCache.setReadOnly(false);
        getContent(handler, url);
        mResponseCache.setReadOnly(true);
        getContent(handler, url);
    }

    /**
     * Tests that the cache file is created atomically.
     * <p>
     * The implementation should write to a temporary file and not move the file
     * to its final location until it has been fully written to prevent another
     * thread from accidentally reading an incomplete cache file.
     */
    public void testAtomicity() throws IOException {
        mResponseCache.setBuffered(false);
        File file = allocateTempFile();
        ContentHandler handler = new NewCacheFileContentHandler(file);
        handler = TestResponseCache.capture(handler, file);
        URL url = new URL("http://www.google.com/");
        Runnable verifier = getContent(handler, url);
        verifier.run();
    }

    /**
     * Tests that partial content is not cached by default.
     */
    public void testPartialContentNotCacheable() throws IOException {
        File file = allocateTempFile();

        // Make sure that the file does not exist before the test.
        assertFalse(file.exists());

        ContentHandler handler = new UncacheableContentHandler(file);
        handler = TestResponseCache.capture(handler, file);
        URL url = new URL("http://farm3.static.flickr.com/2390/2253727548_a413c88ab3_s.jpg");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Range", "bytes=0-999");
        HttpURLConnection http = (HttpURLConnection) connection;
        assertEquals(HttpURLConnection.HTTP_PARTIAL, http.getResponseCode());
        Runnable verifier = getContent(handler, connection);
        verifier.run();
    }

    /**
     * Tests that an error response does not cause an existing cache file to be
     * deleted or overwritten.
     */
    public void testNoOverwrite() throws IOException {
        File file = createTempFile();
        ContentHandler handler = new BasicContentHandler(file);
        handler = TestResponseCache.capture(handler, file);
        URL url = new URL("http://www.google.com/");
        Runnable verifier = getContent(handler, url);
        verifier.run();
        assertTrue(file.exists());
        long length = file.length();
        long timestamp = file.lastModified();

        try {
            // Try caching a URL that always returns an error response
            // to the same cache file that was previously written.
            url = new URL("http://www.google.com/doesnotexist");
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Cache-Control", "max-age=0");
            HttpURLConnection http = (HttpURLConnection) connection;
            int responseCode = http.getResponseCode();
            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
            getContent(handler, connection);
            fail("FileNotFoundException was not thrown");
        } catch (FileNotFoundException e) {
            // Make sure the original file was not overwritten
            // or deleted because an exception was thrown.
            assertTrue(file.exists());
            assertEquals(length, file.length());
            assertEquals(timestamp, file.lastModified());
        }
    }

    /**
     * Caches the content of a {@link URLConnection} while another
     * {@link URLConnection} is being cached.
     * <p>
     * Simulate a scenario where a {@link ContentHandler} creates and invokes a
     * new inner {@link ContentHandler} to fetch dependencies referenced by the
     * content received by the outer {@link ContentHandler}. For example, a web
     * crawler might download images referenced by an HTML document.
     * <p>
     * Test the {@link Stack} inside {@link FileResponseCache}.
     *
     * @throws IOException
     */
    public void testNestedContentHandler() throws IOException {
        File file = createTempFile();
        OuterContentHandler outer = new OuterContentHandler(file);
        ContentHandler handler = TestResponseCache.capture(outer, file);
        URL url = new URL("http://www.google.com/");
        Runnable verifier = getContent(handler, url);
        verifier.run();

        // Make sure the content was cached:

        // Throw an exception if the URLConnection tries writing to the cache
        mResponseCache.setReadOnly(true);

        // This will throw an IOException if the content is not cached
        getContent(handler, url);
    }

    /**
     * Tests {@link FileResponseCache#sink()}.
     * <p>
     * Ensures that the {@link URLConnection} is captured, and that no work is
     * done if the {@link URLConnection} is read from the cache.
     */
    public void testSink() throws IOException {
        AssertViaLocalhostContentHandler assertViaLocalhost;
        URL url = new URL("http://www.google.com/");
        File file = createTempFile();
        ContentHandler handler = TestResponseCache.sink();
        handler = assertViaLocalhost = new AssertViaLocalhostContentHandler(handler);
        handler = TestResponseCache.capture(handler, file);
        assertViaLocalhost.setExpected(false);
        mResponseCache.setReadOnly(false);
        getContent(handler, url);
        assertViaLocalhost.setExpected(true);
        mResponseCache.setReadOnly(true);
        getContent(handler, url);
    }

    private File createTempFile() throws IOException {
        Instrumentation instrumentation = getInstrumentation();
        Context context = instrumentation.getContext();
        File directory = context.getCacheDir();
        File file = File.createTempFile("test", null, directory);
        file.deleteOnExit();
        return file;
    }

    /**
     * Allocates a temporary filename without creating it.
     */
    private File allocateTempFile() throws IOException {
        File file = createTempFile();
        if (!file.delete()) {
            throw new IOException("Unable to delete file");
        }
        if (file.exists()) {
            throw new IOException("File was not deleted");
        }
        return file;
    }

    private static class UnbufferedOutputStream extends FilterOutputStream {

        public UnbufferedOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            super.write(buffer);
            flush();
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {
            super.write(buffer, offset, count);
            flush();
        }

        @Override
        public void write(int oneByte) throws IOException {
            super.write(oneByte);
            flush();
        }
    }

    /**
     * Asserts that {@link SinkContentHandler#isViaLocalhost(URLConnection)}
     * returns the expected value.
     * <p>
     * After performing the assertion, the {@link URLConnection} is passed to an
     * upstream {@link ContentHandler}.
     */
    private static class AssertViaLocalhostContentHandler extends ContentHandler {
        private final ContentHandler mHandler;

        private boolean mExpected;

        public AssertViaLocalhostContentHandler(ContentHandler handler) {
            if (handler == null) {
                throw new NullPointerException();
            }
            mHandler = handler;
        }

        public void setExpected(boolean expected) {
            mExpected = expected;
        }

        @Override
        public Object getContent(URLConnection connection) throws IOException {
            // Establish a connection
            connection.getInputStream();
            assertEquals(mExpected, SinkContentHandler.isViaLocalhost(connection));
            return mHandler.getContent(connection);
        }
    }

    /**
     * Asserts that {@link FileResponseCache#getResponseCode(URLConnection)} and
     * {@link FileResponseCache#getResponseMessage(URLConnection)} return the
     * expected values.
     * <p>
     * The assertion is implemented as a {@link ContentHandler} because the
     * calls must be executed in the context of the {@link ContentHandler}
     * returned by {@link FileResponseCache#capture(ContentHandler, Object)} to
     * enable caching.
     * <p>
     * After performing the assertion, the {@link URLConnection} is passed to an
     * upstream {@link ContentHandler}.
     */
    private static class AssertResponseCodeContentHandler extends ContentHandler {
        private final ContentHandler mHandler;

        private int mExpectedResponseCode;

        private String mExpectedResponseMessage;

        public AssertResponseCodeContentHandler(ContentHandler handler) {
            if (handler == null) {
                throw new NullPointerException();
            }
            mHandler = handler;
        }

        public void setExpected(int responseCode, String responseMessage) {
            mExpectedResponseCode = responseCode;
            mExpectedResponseMessage = responseMessage;
        }

        @Override
        public Object getContent(URLConnection connection) throws IOException {
            assertEquals(mExpectedResponseCode, FileResponseCache.getResponseCode(connection));
            assertEquals(mExpectedResponseMessage, FileResponseCache.getResponseMessage(connection));
            return mHandler.getContent(connection);
        }
    }

    private static class UnbufferedCacheRequest extends CacheRequest {

        private final CacheRequest mCacheRequest;

        public UnbufferedCacheRequest(CacheRequest cacheRequest) {
            mCacheRequest = cacheRequest;
        }

        @Override
        public OutputStream getBody() throws IOException {
            return new UnbufferedOutputStream(mCacheRequest.getBody());
        }

        @Override
        public void abort() {
            mCacheRequest.abort();
        }
    }

    private static class TestResponseCache extends FileResponseCache {

        private boolean mReadOnly;

        private boolean mBuffered = true;

        @Override
        protected File getFile(URI uri, String requestMethod,
                Map<String, List<String>> requestHeaders, Object cookie) {
            // The cache file is always specified as the cookie.
            if (cookie == null) {
                throw new NullPointerException();
            }
            return (File) cookie;
        }

        @Override
        protected boolean isStale(File file, URI uri, String requestMethod,
                Map<String, List<String>> requestHeaders, Object cookie) {
            // File.createTempFile(...) creates an empty file that
            // must not be read as a cached response.
            return file.length() == 0;
        }

        @Override
        public CacheRequest put(URI uri, URLConnection connection) throws IOException {
            if (isReadOnly()) {
                throw new IOException("Cache is read-only");
            }
            CacheRequest request = super.put(uri, connection);
            if (request != null) {
                return mBuffered ? request : new UnbufferedCacheRequest(request);
            } else {
                return null;
            }
        }

        public boolean isReadOnly() {
            return mReadOnly;
        }

        /**
         * Marks the cache as read-only.
         * <p>
         * If any {@link URLConnection} tries to save a response while the cache
         * is read-only, an {@link IOException} will be thrown. This is a good
         * way to check if the network is being accessed as long as all network
         * requests are cached.
         */
        public void setReadOnly(boolean readOnly) {
            mReadOnly = readOnly;
        }

        /**
         * Enables or disables {@link FileOutputStream} buffering.
         */
        public void setBuffered(boolean buffered) {
            mBuffered = buffered;
        }
    }

    /**
     * A {@link ContentHandler} that reads and caches a {@link URLConnection}
     * while making an inner request using a second {@link ContentHandler}.
     */
    private class OuterContentHandler extends ContentHandler implements Runnable {

        private final File mFile;

        private BasicContentHandler mInnerContentHandler;

        private ContentHandler mHandler;

        public OuterContentHandler(File file) {
            mFile = file;
        }

        private ContentHandler getInnerContentHandler() throws IOException {
            if (mHandler == null) {
                File file = createTempFile();
                mInnerContentHandler = new BasicContentHandler(file);
                mHandler = TestResponseCache.capture(mInnerContentHandler, file);
            }
            return mHandler;
        }

        @Override
        public Object getContent(URLConnection conn) throws IOException {
            InputStream input = conn.getInputStream();
            try {
                while (input.read() != -1) {
                }

                // Make an inner request while the outer request is still
                // active to test the stack in FileResponseCache.
                ContentHandler handler = getInnerContentHandler();
                URL url = new URL("http://www.google.com/intl/en_us/images/logo.gif");
                URLConnection connection = url.openConnection();
                return handler.getContent(connection);
            } finally {
                input.close();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            assertTrue(mFile.exists());
            assertFalse(mFile.length() == 0);
            mInnerContentHandler.run();
        }
    }

    /**
     * A {@link ContentHandler} that reads a {@link URLConnection}.
     */
    private static class BasicContentHandler extends ContentHandler implements Runnable {

        private final File mFile;

        /**
         * @param file the file that will be used for caching.
         */
        public BasicContentHandler(File file) {
            mFile = file;
        }

        @Override
        public Object getContent(URLConnection conn) throws IOException {
            InputStream input = conn.getInputStream();
            try {
                while (input.read() != -1) {
                }
            } finally {
                input.close();
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            assertTrue(mFile.exists());
            assertFalse(mFile.length() == 0);
        }
    }

    /**
     * A {@link ContentHandler} that reads a {@link URLConnection} that should
     * not be cached and returns a {@link Runnable} that will verify that the
     * cache file was not created.
     */
    private static class UncacheableContentHandler extends ContentHandler implements Runnable {

        private final File mFile;

        /**
         * @param file the file that will be used for caching.
         */
        public UncacheableContentHandler(File file) {
            mFile = file;
        }

        @Override
        public Object getContent(URLConnection conn) throws IOException {
            InputStream input = conn.getInputStream();
            try {
                while (input.read() != -1) {
                }
            } finally {
                input.close();
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            assertFalse(mFile.exists());
        }
    }

    /**
     * A {@link ContentHandler} that ensures that the cache file does not exist
     * before it has been fully written.
     */
    private static class NewCacheFileContentHandler extends ContentHandler implements Runnable {

        private final File mFile;

        /**
         * Constructor.
         * 
         * @param file the file that will be used for caching.
         */
        public NewCacheFileContentHandler(File file) {
            mFile = file;
        }

        @Override
        public Object getContent(URLConnection conn) throws IOException {
            assertFalse(mFile.exists());
            InputStream input = conn.getInputStream();
            try {
                assertFalse(mFile.exists());

                // Buffer reads for performance
                byte[] buffer = new byte[256];

                while (input.read(buffer) != -1) {
                    if (mFile.exists()) {
                        boolean atEnd = (input.read() < 0);
                        assertTrue(atEnd);
                    }
                }
            } finally {
                input.close();
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            assertTrue(mFile.exists());
            assertFalse(mFile.length() == 0);
        }
    }
}
