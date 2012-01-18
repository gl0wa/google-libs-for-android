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

package com.google.android.imageloader;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ContentHandler;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

/**
 * A helper class to executeAppendTask images asynchronously.
 */
public final class ImageLoader {

    private static final String TAG = "ImageLoader";

    /**
     * The default maximum number of active tasks.
     */
    public static final int DEFAULT_TASK_LIMIT = 3;

    /**
     * The default cache size (in bytes).
     */
    // 25% of available memory, up to a maximum of 16MB
    public static final long DEFAULT_CACHE_SIZE = Math.min(Runtime.getRuntime().maxMemory() / 4,
            16 * 1024 * 1024);

    /**
     * Use with {@link Context#getSystemService(String)} to retrieve an
     * {@link ImageLoader} for loading images.
     * <p/>
     * Since {@link ImageLoader} is not a standard system service, you must
     * create a custom {@link Application} subclass implementing
     * {@link Application#getSystemService(String)} and add it to your
     * {@code AndroidManifest.xml}.
     * <p/>
     * Using this constant is optional and it is only provided for convenience
     * and to promote consistency across deployments of this component.
     */
    public static final String IMAGE_LOADER_SERVICE = "com.google.android.imageloader";

    private final ArrayList<ImageCallback> mPendingCallbacks = new ArrayList<ImageCallback>();
    private boolean mPaused;

    /**
     * Gets the {@link ImageLoader} from a {@link Context}.
     *
     * @throws IllegalStateException if the {@link Application} does not have an
     *                               {@link ImageLoader}.
     * @see #IMAGE_LOADER_SERVICE
     */
    public static ImageLoader get(Context context) {
        ImageLoader loader = (ImageLoader) context.getSystemService(IMAGE_LOADER_SERVICE);
        if (loader == null) {
            context = context.getApplicationContext();
            loader = (ImageLoader) context.getSystemService(IMAGE_LOADER_SERVICE);
        }
        if (loader == null) {
            throw new IllegalStateException("ImageLoader not available");
        }
        return loader;
    }

    public void pause() {
        mPaused = true;
    }

    public void unpause() {
        if (!mPaused) return;
        mPaused = false;
        flushRequests();
        for (ImageCallback imageCallback : mPendingCallbacks) {
            imageCallback.send();
        }
        mPendingCallbacks.clear();
    }

    /**
     * Callback interface for executeAppendTask and error events.
     * <p/>
     * This interface is only applicable when binding a stand-alone
     * {@link ImageView}. When the target {@link ImageView} is in an
     * {@link AdapterView},
     * {@link ImageLoader#bind(BaseAdapter, ImageView, String, Options)} will be called
     * implicitly by {@link BaseAdapter#notifyDataSetChanged()}.
     */
    public interface Callback {
        /**
         * Notifies an observer that an image was loaded.
         * <p/>
         * The bitmap will be assigned to the {@link ImageView} automatically.
         * <p/>
         * Use this callback to dismiss any loading indicators.
         *
         * @param view the {@link ImageView} that was loaded.
         * @param url  the URL that was loaded.
         */
        void onImageLoaded(ImageView view, String url);

        /**
         * Notifies an observer that an image could not be loaded.
         *
         * @param view  the {@link ImageView} that could not be loaded.
         * @param url   the URL that could not be loaded.
         * @param error the exception that was thrown.
         */
        void onImageError(ImageView view, String url, Throwable error);
    }

    public static enum BindResult {
        /**
         * Returned when an image is bound to an {@link ImageView} immediately
         * because it was already loaded.
         */
        OK,
        /**
         * Returned when an image needs to be loaded asynchronously.
         * <p/>
         * Callers may wish to assign a placeholder or show a progress spinner
         * while the image is being loaded whenever this value is returned.
         */
        LOADING,
        /**
         * Returned when an attempt to executeAppendTask the image has already been made and
         * it failed.
         * <p/>
         * Callers may wish to show an error indicator when this value is
         * returned.
         *
         * @see ImageLoader.Callback
         */
        ERROR
    }

    private static String getProtocol(String url) {
        Uri uri = Uri.parse(url);
        return uri.getScheme();
    }

    private final ContentHandler mBitmapContentHandler;

    private final ContentHandler mPrefetchContentHandler;

    private final URLStreamHandlerFactory mURLStreamHandlerFactory;

    private final HashMap<String, URLStreamHandler> mStreamHandlers;

    private final LinkedList<ImageRequest> mRequests;

    /**
     * A cache containing recently used bitmaps.
     * <p/>
     * Use soft references so that the application does not run out of memory in
     * the case where one or more of the bitmaps are large.
     */
    private final Map<String, Bitmap> mBitmaps;

    /**
     * Recent errors encountered when loading bitmaps.
     */
    private final Map<String, ImageError> mErrors;

    /**
     * Tracks the last URL that was bound to an {@link ImageView}.
     * <p/>
     * This ensures that the right image is shown in the case where a new URL is
     * assigned to an {@link ImageView} before the previous asynchronous task
     * completes.
     * <p/>
     * This <em>does not</em> ensure that an image assigned with
     * {@link ImageView#setImageBitmap(Bitmap)},
     * {@link ImageView#setImageDrawable(android.graphics.drawable.Drawable)},
     * {@link ImageView#setImageResource(int)}, or
     * {@link ImageView#setImageURI(android.net.Uri)} is not replaced. This
     * behavior is important because callers may invoke these methods to assign
     * a placeholder when a bind method returns {@link BindResult#LOADING} or
     * {@link BindResult#ERROR}.
     */
    private final Map<ImageView, String> mImageViewBinding;

    /**
     * The maximum number of active tasks.
     */
    private final int mMaxTaskCount;

    /**
     * The current number of active tasks.
     */
    private int mActiveTaskCount;

    /**
     * Creates an {@link ImageLoader}.
     *
     * @param taskLimit       the maximum number of background tasks that may be
     *                        active at one time.
     * @param streamFactory   a {@link URLStreamHandlerFactory} for creating
     *                        connections to special URLs such as {@code content://} URIs.
     *                        This parameter can be {@code null} if the {@link ImageLoader}
     *                        only needs to executeAppendTask images over HTTP or if a custom
     *                        {@link URLStreamHandlerFactory} has already been passed to
     *                        {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}
     * @param bitmapHandler   a {@link ContentHandler} for loading images.
     *                        {@link ContentHandler#getContent(URLConnection)} must either
     *                        return a {@link Bitmap} or throw an {@link IOException}. This
     *                        parameter can be {@code null} to use the default
     *                        {@link BitmapContentHandler}.
     * @param prefetchHandler a {@link ContentHandler} for caching a remote URL
     *                        as a file, without parsing it or loading it into memory.
     *                        {@link ContentHandler#getContent(URLConnection)} should always
     *                        return {@code null}. If the URL passed to the
     *                        {@link ContentHandler} is already local (for example,
     *                        {@code file://}), this {@link ContentHandler} should do
     *                        nothing. The {@link ContentHandler} can be {@code null} if
     *                        pre-fetching is not required.
     * @param cacheSize       the maximum size of the image cache (in bytes).
     * @param handler         a {@link Handler} identifying the callback thread, or
     *                        {@code} null for the main thread.
     * @throws NullPointerException if the factory is {@code null}.
     */
    public ImageLoader(int taskLimit, URLStreamHandlerFactory streamFactory,
                       ContentHandler bitmapHandler, ContentHandler prefetchHandler, long cacheSize,
                       Handler handler) {
        if (taskLimit < 1) {
            throw new IllegalArgumentException("Task limit must be positive");
        }
        if (cacheSize < 1) {
            throw new IllegalArgumentException("Cache size must be positive");
        }
        mMaxTaskCount = taskLimit;
        mURLStreamHandlerFactory = streamFactory;
        mStreamHandlers = streamFactory != null ? new HashMap<String, URLStreamHandler>() : null;
        mBitmapContentHandler = bitmapHandler != null ? bitmapHandler : new BitmapContentHandler();
        mPrefetchContentHandler = prefetchHandler;

        mImageViewBinding = new WeakHashMap<ImageView, String>();

        mRequests = new LinkedList<ImageRequest>();

        // Use a LruCache to prevent the set of keys from growing too large.
        // The Maps must be synchronized because they are accessed
        // by the UI thread and by background threads.
        mBitmaps = Collections.synchronizedMap(new BitmapCache<String>(cacheSize));
        mErrors = Collections.synchronizedMap(new LruCache<String, ImageError>());
    }

    /**
     * Creates a basic {@link ImageLoader} with support for HTTP URLs and
     * in-memory caching.
     * <p/>
     * Persistent caching and content:// URIs are not supported when this
     * constructor is used.
     */
    public ImageLoader() {
        this(DEFAULT_TASK_LIMIT, null, null, null, DEFAULT_CACHE_SIZE, null);
    }

    /**
     * Creates a basic {@link ImageLoader} with support for HTTP URLs and
     * in-memory caching.
     * <p/>
     * Persistent caching and content:// URIs are not supported when this
     * constructor is used.
     *
     * @param taskLimit the maximum number of background tasks that may be
     *                  active at a time.
     */
    public ImageLoader(int taskLimit) {
        this(taskLimit, null, null, null, DEFAULT_CACHE_SIZE, null);
    }

    /**
     * Creates a basic {@link ImageLoader} with support for HTTP URLs and
     * in-memory caching.
     * <p/>
     * Persistent caching and content:// URIs are not supported when this
     * constructor is used.
     *
     * @param cacheSize the maximum size of the image cache (in bytes).
     */
    public ImageLoader(long cacheSize) {
        this(DEFAULT_TASK_LIMIT, null, null, null, cacheSize, null);
    }

    /**
     * Creates an {@link ImageLoader} with support for pre-fetching.
     *
     * @param bitmapHandler   a {@link ContentHandler} that reads, caches, and
     *                        returns a {@link Bitmap}.
     * @param prefetchHandler a {@link ContentHandler} for caching a remote URL
     *                        as a file, without parsing it or loading it into memory.
     *                        {@link ContentHandler#getContent(URLConnection)} should always
     *                        return {@code null}. If the URL passed to the
     *                        {@link ContentHandler} is already local (for example,
     *                        {@code file://}), this {@link ContentHandler} should return
     *                        {@code null} immediately.
     */
    public ImageLoader(ContentHandler bitmapHandler, ContentHandler prefetchHandler) {
        this(DEFAULT_TASK_LIMIT, null, bitmapHandler, prefetchHandler, DEFAULT_CACHE_SIZE, null);
    }

    /**
     * Creates an {@link ImageLoader} with support for http:// and content://
     * URIs.
     * <p/>
     * Prefetching is not supported when this constructor is used.
     *
     * @param resolver a {@link ContentResolver} for accessing content:// URIs.
     */
    public ImageLoader(ContentResolver resolver) {
        this(DEFAULT_TASK_LIMIT, new ContentURLStreamHandlerFactory(resolver), null, null,
                DEFAULT_CACHE_SIZE, null);
    }

    /**
     * Creates an {@link ImageLoader} with a custom
     * {@link URLStreamHandlerFactory}.
     * <p/>
     * Use this constructor when loading images with protocols other than
     * {@code http://} and when a custom {@link URLStreamHandlerFactory} has not
     * already been installed with
     * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}. If the
     * only additional protocol support required is for {@code content://} URIs,
     * consider using {@link #ImageLoader(ContentResolver)}.
     * <p/>
     * Prefetching is not supported when this constructor is used.
     */
    public ImageLoader(URLStreamHandlerFactory factory) {
        this(DEFAULT_TASK_LIMIT, factory, null, null, DEFAULT_CACHE_SIZE, null);
    }

    private URLStreamHandler getURLStreamHandler(String protocol) {
        URLStreamHandlerFactory factory = mURLStreamHandlerFactory;
        if (factory == null) {
            return null;
        }
        HashMap<String, URLStreamHandler> handlers = mStreamHandlers;
        synchronized (handlers) {
            URLStreamHandler handler = handlers.get(protocol);
            if (handler == null) {
                handler = factory.createURLStreamHandler(protocol);
                if (handler != null) {
                    handlers.put(protocol, handler);
                }
            }
            return handler;
        }
    }

    /**
     * Creates tasks to service any pending requests until {@link #mRequests} is
     * empty or {@link #mMaxTaskCount} is reached.
     */
    void flushRequests() {
        if (!mPaused)
            while (mActiveTaskCount < mMaxTaskCount && !mRequests.isEmpty()) {
                new ImageTask().executeOnThreadPool(mRequests.poll());
            }
    }

    public void cancelRequest(String url) {
        ImageRequest toRemove = null;
        for (ImageRequest request : mRequests) {
            if (request.getUrl().equals(url)) toRemove = request;
        }
        if (toRemove != null) mRequests.remove(toRemove);
    }

    private void enqueueRequest(ImageRequest request) {
        mRequests.add(request);
        flushRequests();
    }

    private void insertRequestAtFrontOfQueue(ImageRequest request) {
        mRequests.add(0, request);
        flushRequests();
    }

    public static class Options {

        public Options() {
            this.loadRemotelyIfNecessary = true;
            this.decodeInSampleSize = 1;
        }

        public Options(boolean loadRemotelyIfNecessary) {
            this.loadRemotelyIfNecessary = loadRemotelyIfNecessary;
            this.decodeInSampleSize = 1;
        }

        public Options(boolean loadRemotelyIfNecessary, boolean postAtFront) {
            this.loadRemotelyIfNecessary = loadRemotelyIfNecessary;
            this.postAtFront = postAtFront;
            this.decodeInSampleSize = 1;
        }

        public final boolean loadRemotelyIfNecessary;
        public boolean postAtFront;
        public final int decodeInSampleSize;
        public int cornerRadius;
        public WeakReference<Bitmap> temporaryBitmapRef;
    }

    public Bitmap getBitmap(String uri, BitmapCallback callback) {
        return this.getBitmap(uri, callback, new Options());
    }

    public Bitmap getBitmap(String uri, BitmapCallback callback, Options options) {
        if (options == null) options = new Options();
        Bitmap memoryBmp = getBitmap(uri);
        if (getBitmap(uri) != null) {
            if (callback != null) {
                callback.setResult(uri, memoryBmp, null);
                callback.send();
            }
            return memoryBmp;
        } else if (options.loadRemotelyIfNecessary) {
            ImageRequest request = new ImageRequest(uri, callback, true, options);
            if (options.postAtFront) {
                insertRequestAtFrontOfQueue(request);
            } else {
                enqueueRequest(request);
            }
        }
        return null;
    }

    /**
     * Binds a URL to an {@link ImageView} within an {@link android.widget.AdapterView}.
     *
     * @param adapter the adapter for the {@link android.widget.AdapterView}.
     * @param view    the {@link ImageView}.
     * @param url     the image URL.
     * @return a {@link BindResult}.
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
    public BindResult bind(BaseAdapter adapter, ImageView view, String url) {
        return this.bind(adapter, view, url, new Options());
    }

    public BindResult bind(BaseAdapter adapter, ImageView view, String url, Options options) {
        if (adapter == null) {
            throw new NullPointerException("Adapter is null");
        }
        if (view == null) {
            throw new NullPointerException("ImageView is null");
        }
        if (url == null) {
            throw new NullPointerException("URL is null");
        }
        Bitmap bitmap = getBitmap(url);
        ImageError error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default.
            // The caller can set their own placeholder
            // based on the return value.
            view.setImageDrawable(null);

            if (error != null) {
                return BindResult.ERROR;
            } else {
                ImageRequest request = new ImageRequest(adapter, url, options);

                // For adapters, post the latest requests
                // at the front of the queue in case the user
                // has already scrolled past most of the images
                // that are currently in the queue.
                insertRequestAtFrontOfQueue(request);

                return BindResult.LOADING;
            }
        }
    }

    /**
     * Binds a URL to an {@link ImageView} within an {@link android.widget.ExpandableListView}.
     *
     * @param adapter the adapter for the {@link android.widget.ExpandableListView}.
     * @param view    the {@link ImageView}.
     * @param url     the image URL.
     * @return a {@link BindResult}.
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
    public BindResult bind(BaseExpandableListAdapter adapter, ImageView view, String url) {
        return this.bind(adapter, view, url);
    }

    public BindResult bind(BaseExpandableListAdapter adapter, ImageView view, String url, Options options) {
        if (adapter == null) {
            throw new NullPointerException("Adapter is null");
        }
        if (view == null) {
            throw new NullPointerException("ImageView is null");
        }
        if (url == null) {
            throw new NullPointerException("URL is null");
        }
        Bitmap bitmap = getBitmap(url);
        ImageError error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default.
            // The caller can set their own placeholder
            // based on the return value.
            view.setImageDrawable(null);

            if (error != null) {
                return BindResult.ERROR;
            } else {
                ImageRequest request = new ImageRequest(adapter, url, options);

                // For adapters, post the latest requests
                // at the front of the queue in case the user
                // has already scrolled past most of the images
                // that are currently in the queue.
                insertRequestAtFrontOfQueue(request);

                return BindResult.LOADING;
            }
        }
    }

    /**
     * Binds an image at the given URL to an {@link ImageView}.
     * <p/>
     * If the image needs to be loaded asynchronously, it will be assigned at a
     * later time, replacing any existing {@link Drawable} unless
     * {@link #unbind(ImageView)} is called or
     * {@link #bind(ImageView, String, Callback, Options)} is called with the same
     * {@link ImageView}, but a different URL.
     * <p/>
     * Use {@link #bind(BaseAdapter, ImageView, String, Options)} instead of this method
     * when the {@link ImageView} is in an {@link android.widget.AdapterView} so that the image
     * will be bound correctly in the case where it has been assigned to a
     * different position since the asynchronous request was started.
     *
     * @param view     the {@link ImageView} to bind.
     * @param url      the image URL.s
     * @param callback invoked when there is an error loading the image. This
     *                 parameter can be {@code null} if a callback is not required.
     * @return a {@link BindResult}.
     * @throws NullPointerException if a required argument is {@code null}
     */
    public BindResult bind(ImageView view, String url, Callback callback) {
        return this.bind(view, url, callback, new Options());
    }

    public BindResult bind(ImageView view, String url, Callback callback, Options options) {
        if (view == null) {
            throw new NullPointerException("ImageView is null");
        }
        if (url == null) {
            throw new NullPointerException("URL is null");
        }
        mImageViewBinding.put(view, url);
        Bitmap bitmap = getBitmap(url);
        ImageError error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default.
            // The caller can set their own placeholder
            // based on the return value.
            final Bitmap temporaryBitmap = options.temporaryBitmapRef != null ? options.temporaryBitmapRef.get() : null;
            if (temporaryBitmap != null) {
                view.setImageBitmap(temporaryBitmap);
            } else {
                view.setImageDrawable(null);
            }

            if (error != null) {
                return BindResult.ERROR;
            } else {
                ImageRequest request = new ImageRequest(view, url, callback, options);
                if (options.postAtFront) {
                    insertRequestAtFrontOfQueue(request);
                } else {
                    enqueueRequest(request);
                }

                return BindResult.LOADING;
            }
        }
    }

    /**
     * Cancels an asynchronous request to bind an image URL to an
     * {@link ImageView} and clears the {@link ImageView}.
     *
     * @see #bind(ImageView, String, Callback, Options)
     */
    public void unbind(ImageView view) {
        mImageViewBinding.remove(view);
        view.setImageDrawable(null);
    }

    /**
     * Clears any cached errors.
     * <p/>
     * Call this method when a network connection is restored, or the user
     * invokes a manual executeRefreshTask of the screen.
     */
    public void clearErrors() {
        mErrors.clear();
    }

    /**
     * Pre-loads an image into memory.
     * <p/>
     * The image may be unloaded if memory is low. Use {@link #prefetch(String, Options)}
     * and a file-based cache to pre-executeAppendTask more images.
     *
     * @param url the image URL
     * @throws NullPointerException if the URL is {@code null}
     */
    public void preload(String url, Options options) {
        if (url == null) {
            throw new NullPointerException();
        }
        if (null != getBitmap(url)) {
            // The image is already loaded
            return;
        }
        if (null != getError(url)) {
            // A recent attempt to executeAppendTask the image failed,
            // therefore this attempt is likely to fail as well.
            return;
        }
        boolean loadBitmap = true;
        ImageRequest task = new ImageRequest(url, loadBitmap, options);
        enqueueRequest(task);
    }

    /**
     * Pre-loads a range of images into memory from a {@link Cursor}.
     * <p/>
     * Typically, an {@link Activity} would register a {@link DataSetObserver}
     * and an {@link android.widget.AdapterView.OnItemSelectedListener}, then
     * call this method to prime the in-memory cache with images adjacent to the
     * current selection whenever the selection or data changes.
     * <p/>
     * Any invalid positions in the specified range will be silently ignored.
     *
     * @param cursor      a {@link Cursor} containing the image URLs.
     * @param columnIndex the column index of the image URL. The column value
     *                    may be {@code NULL}.
     * @param start       the first position to executeAppendTask. For example, {@code
     *                    selectedPosition - 5}.
     * @param end         the first position not to executeAppendTask. For example, {@code
     *                    selectedPosition + 5}.
     * @see #preload(String, Options)
     */
    public void preload(Cursor cursor, int columnIndex, int start, int end, Options options) {
        for (int position = start; position < end; position++) {
            if (cursor.moveToPosition(position)) {
                String url = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(url)) {
                    preload(url, options);
                }
            }
        }
    }

    /**
     * Pre-fetches the binary content for an image and stores it in a file-based
     * cache (if it is not already cached locally) without loading the image
     * data into memory.
     * <p/>
     * Pre-fetching should not be used unless a {@link ContentHandler} with
     * support for persistent caching was passed to the constructor.
     *
     * @param url the URL to pre-fetch.
     * @throws NullPointerException if the URL is {@code null}
     */
    public void prefetch(String url) {
        prefetch(url, new Options());
    }

    public void prefetch(String url, Options options) {
        if (url == null) {
            throw new NullPointerException();
        }
        if (null != getBitmap(url)) {
            // The image is already loaded, therefore
            // it does not need to be prefetched.
            return;
        }
        if (null != getError(url)) {
            // A recent attempt to executeAppendTask or prefetch the image failed,
            // therefore this attempt is likely to fail as well.
            return;
        }
        boolean loadBitmap = false;
        ImageRequest request = new ImageRequest(url, loadBitmap, options);
        enqueueRequest(request);
    }

    /**
     * Pre-fetches the binary content for images referenced by a {@link Cursor},
     * without loading the image data into memory.
     * <p/>
     * Pre-fetching should not be used unless a {@link ContentHandler} with
     * support for persistent caching was passed to the constructor.
     * <p/>
     * Typically, an {@link Activity} would register a {@link DataSetObserver}
     * and call this method from {@link DataSetObserver#onChanged()} to executeAppendTask
     * off-screen images into a file-based cache when they are not already
     * present in the cache.
     *
     * @param cursor      the {@link Cursor} containing the image URLs.
     * @param columnIndex the column index of the image URL. The column value
     *                    may be {@code NULL}.
     * @see #prefetch(String, Options)
     */
    public void prefetch(Cursor cursor, int columnIndex, Options options) {
        for (int position = 0; cursor.moveToPosition(position); position++) {
            String url = cursor.getString(columnIndex);
            if (!TextUtils.isEmpty(url)) {
                prefetch(url, options);
            }
        }
    }

    private void putBitmap(String url, Bitmap bitmap) {
        mBitmaps.put(url, bitmap);
    }

    private void putError(String url, ImageError error) {
        mErrors.put(url, error);
    }

    private Bitmap getBitmap(String url) {
        return mBitmaps.get(url);
    }

    private ImageError getError(String url) {
        ImageError error = mErrors.get(url);
        return error != null && !error.isExpired() ? error : null;
    }

    /**
     * Returns {@code true} if there was an error the last time the given URL
     * was accessed and the error is not expired, {@code false} otherwise.
     */
    private boolean hasError(String url) {
        return getError(url) != null;
    }

    private class ImageRequest {

        private final BitmapCallback mBitmapCallback;

        private final ImageCallback mImageViewCallback;

        private final String mUrl;

        private final Options mOptions;

        private final boolean mLoadBitmap;

        private Bitmap mBitmap;

        private ImageError mError;


        private ImageRequest(String url, ImageCallback callback, boolean loadBitmap, Options options) {
            mUrl = url;
            mImageViewCallback = callback;
            mLoadBitmap = loadBitmap;
            mOptions = null;
            mBitmapCallback = null;
        }

        /**
         * Creates an {@link ImageTask} to executeAppendTask a {@link Bitmap} for an
         * {@link ImageView} in an {@link android.widget.AdapterView}.
         */
        public ImageRequest(BaseAdapter adapter, String url, Options options) {
            this(url, new BaseAdapterCallback(adapter), true, options);
        }

        /**
         * Creates an {@link ImageTask} to executeAppendTask a {@link Bitmap} for an
         * {@link ImageView} in an {@link android.widget.ExpandableListView}.
         */
        public ImageRequest(BaseExpandableListAdapter adapter, String url, Options options) {
            this(url, new BaseExpandableListAdapterCallback(adapter), true, options);
        }

        /**
         * Creates an {@link ImageTask} to executeAppendTask a {@link Bitmap} for an
         * {@link ImageView}.
         */
        public ImageRequest(ImageView view, String url, Callback callback, Options options) {
            this(url, new ImageViewCallback(view, callback), true, options);
        }

        /**
         * Creates an {@link ImageTask} to prime the cache.
         */
        public ImageRequest(String url, boolean loadBitmap, Options options) {
            this(url, null, loadBitmap, options);
        }

        private Bitmap loadImage(URL url, Options options) throws IOException {
            URLConnection connection = url.openConnection();
            return processBitmap((Bitmap) mBitmapContentHandler.getContent(connection), options);
        }

        /**
         * Executes the {@link ImageTask}.
         *
         * @return {@code true} if the result for this {@link ImageTask} should
         *         be posted, {@code false} otherwise.
         */
        public boolean execute() {
            try {
                if (mImageViewCallback != null) {
                    if (mImageViewCallback.unwanted()) {
                        return false;
                    }
                }
                // Check if the last attempt to executeAppendTask the URL had an error
                mError = getError(mUrl);
                if (mError != null) {
                    return true;
                }

                // Check if the Bitmap is already cached in memory
                mBitmap = getBitmap(mUrl);
                if (mBitmap != null) {
                    // Keep a hard reference until the view has been notified.
                    return true;
                } else if (new File(mUrl).exists()) {
                    BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
                    sampleOptions.inSampleSize = mOptions.decodeInSampleSize;
                    mBitmap = processBitmap(BitmapFactory.decodeFile(mUrl, sampleOptions), mOptions);
                    return true;
                }

                String protocol = getProtocol(mUrl);
                URLStreamHandler streamHandler = getURLStreamHandler(protocol);
                URL url = new URL(null, mUrl, streamHandler);

                if (mLoadBitmap) {
                    try {
                        mBitmap = loadImage(url, mOptions);
                    } catch (OutOfMemoryError e) {
                        // The VM does not always free-up memory as it should,
                        // so manually invoke the garbage collector
                        // and try loading the image again.
                        System.gc();
                        mBitmap = loadImage(url, mOptions);
                    }
                    if (mBitmap == null) {
                        throw new NullPointerException("ContentHandler returned null");
                    }
                    return true;
                } else {
                    if (mPrefetchContentHandler != null) {
                        // Cache the URL without loading a Bitmap into memory.
                        URLConnection connection = url.openConnection();
                        mPrefetchContentHandler.getContent(connection);
                    }
                    mBitmap = null;
                    return false;
                }
            } catch (IOException e) {
                mError = new ImageError(e);
                return true;
            } catch (RuntimeException e) {
                mError = new ImageError(e);
                return true;
            } catch (Error e) {
                mError = new ImageError(e);
                return true;
            }
        }

        public void publishResult() {
            if (mBitmap != null) {
                putBitmap(mUrl, mBitmap);
            } else if (mError != null && !hasError(mUrl)) {
                putError(mUrl, mError);
            }
            if (mImageViewCallback != null) {
                mImageViewCallback.setResult(mUrl, mBitmap, mError);
                if (!mPaused) {
                    mImageViewCallback.send();
                } else {
                    mPendingCallbacks.add(mImageViewCallback);
                }
            }
        }

        public String getUrl() {
            return mUrl;
        }
    }

    private interface ImageCallback {
        boolean unwanted();

        void setResult(String url, Bitmap bitmap, ImageError imageError);

        void send();
    }

    public interface ISchedulableAdapter {
        void scheduleNotifyDataSetChanged();
    }

    public static class BitmapCallback implements ImageCallback {
        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        @Override
        public boolean unwanted() {
            return false;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        /**
         * {@inheritDoc}
         */
        public void send() {
            if (mError == null) {
                onImageLoaded(mBitmap, mUrl);
            } else {
                onImageError(mUrl, mError.getCause());
            }
        }

        public void onImageLoaded(Bitmap mBitmap, String uri) {

        }

        public void onImageError(String uri, Throwable error) {

        }

    }

    public final class ImageViewCallback implements ImageCallback {

        // TODO: Use WeakReferences?
        private final WeakReference<ImageView> mImageView;
        private final Callback mCallback;

        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        public ImageViewCallback(ImageView imageView, Callback callback) {
            mImageView = new WeakReference<ImageView>(imageView);
            mCallback = callback;
        }

        /**
         * {@inheritDoc}
         */
        public boolean unwanted() {
            // Always complete the callback
            return false;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        /**
         * {@inheritDoc}
         */
        public void send() {
            final ImageView imageView = mImageView.get();
            if (imageView == null) return;

            String binding = mImageViewBinding.get(imageView);
            if (!TextUtils.equals(binding, mUrl)) {
                // The ImageView has been unbound or bound to a
                // different URL since the task was started.
                return;
            }

            Context context = imageView.getContext();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity.isFinishing()) {
                    return;
                }
            }
            if (mBitmap != null) {
                imageView.setImageBitmap(mBitmap);
                if (mCallback != null) {
                    mCallback.onImageLoaded(imageView, mUrl);
                }
            } else if (mError != null) {
                if (mCallback != null) {
                    mCallback.onImageError(imageView, mUrl, mError.getCause());
                }
            }
        }
    }

    private static final class BaseAdapterCallback implements ImageCallback {
        private final WeakReference<BaseAdapter> mAdapter;

        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        public BaseAdapterCallback(BaseAdapter adapter) {
            mAdapter = new WeakReference<BaseAdapter>(adapter);
        }

        /**
         * {@inheritDoc}
         */
        public boolean unwanted() {
            return mAdapter.get() == null;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        /**
         * {@inheritDoc}
         */
        public void send() {
            BaseAdapter adapter = mAdapter.get();
            if (adapter == null) {
                // The adapter is no longer in use
                return;
            }
            if (!adapter.isEmpty()) {
                if (adapter instanceof ISchedulableAdapter) {
                    ((ISchedulableAdapter) adapter).scheduleNotifyDataSetChanged();
                } else {
                    adapter.notifyDataSetChanged();
                }
            } else {
                // The adapter is empty or no longer in use.
                // It is important that BaseAdapter#notifyDataSetChanged()
                // is not called when the adapter is empty because this
                // may indicate that the data is valid when it is not.
                // For example: when the adapter cursor is deactivated.
            }
        }
    }

    private static final class BaseExpandableListAdapterCallback implements ImageCallback {

        private final WeakReference<BaseExpandableListAdapter> mAdapter;

        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        public BaseExpandableListAdapterCallback(BaseExpandableListAdapter adapter) {
            mAdapter = new WeakReference<BaseExpandableListAdapter>(adapter);
        }

        /**
         * {@inheritDoc}
         */
        public boolean unwanted() {
            return mAdapter.get() == null;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        /**
         * {@inheritDoc}
         */
        public void send() {
            BaseExpandableListAdapter adapter = mAdapter.get();
            if (adapter == null) {
                // The adapter is no longer in use
                return;
            }
            if (!adapter.isEmpty()) {
                adapter.notifyDataSetChanged();
            } else {
                // The adapter is empty or no longer in use.
                // It is important that BaseAdapter#notifyDataSetChanged()
                // is not called when the adapter is empty because this
                // may indicate that the data is valid when it is not.
                // For example: when the adapter cursor is deactivated.
            }
        }
    }

    private class ImageTask extends AsyncTask<ImageRequest, ImageRequest, Void> {

        public final android.os.AsyncTask<ImageRequest, ImageRequest, Void> executeOnThreadPool(
                ImageRequest... params) {
            if (Build.VERSION.SDK_INT < 4) {
                // Thread pool size is 1
                return execute(params);
            } else if (Build.VERSION.SDK_INT < 11) {
                // The execute() method uses a thread pool
                return execute(params);
            } else {
                // The execute() method uses a single thread,
                // so call executeOnExecutor() instead.
                try {
                    Method method = android.os.AsyncTask.class.getMethod("executeOnExecutor",
                            Executor.class, Object[].class);
                    Field field = android.os.AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
                    Object executor = field.get(null);
                    method.invoke(this, executor, params);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Unexpected NoSuchMethodException", e);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Unexpected NoSuchFieldException", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unexpected IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Unexpected InvocationTargetException", e);
                }
                return this;
            }
        }

        @Override
        protected void onPreExecute() {
            mActiveTaskCount++;
        }

        @Override
        protected Void doInBackground(ImageRequest... requests) {
            for (ImageRequest request : requests) {
                if (request.execute()) {
                    publishProgress(request);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(ImageRequest... values) {
            for (ImageRequest request : values) {
                request.publishResult();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            mActiveTaskCount--;
            flushRequests();
        }
    }

    private static class ImageError {
        private static final int TIMEOUT = 2 * 60 * 1000; // Two minutes

        private final Throwable mCause;

        private final long mTimestamp;

        public ImageError(Throwable cause) {
            if (cause == null) {
                throw new NullPointerException();
            }
            mCause = cause;
            mTimestamp = now();
        }

        public boolean isExpired() {
            return (now() - mTimestamp) > TIMEOUT;
        }

        public Throwable getCause() {
            return mCause;
        }

        private static long now() {
            return SystemClock.elapsedRealtime();
        }
    }

    private static Bitmap processBitmap(Bitmap bitmap, Options options) {
        if (options == null) return bitmap;
        if (options.cornerRadius > 0) {
            Bitmap old = bitmap;
            bitmap = getRoundedCornerBitmap(old, options.cornerRadius);
            old.recycle();
        }
        return bitmap;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}