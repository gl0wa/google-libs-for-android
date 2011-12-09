/*-
 * Copyright (C) 2011 Google Inc.
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

import com.google.android.imageloader.ImageLoader.BindResult;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.ImageView;

import java.util.concurrent.CountDownLatch;

/**
 * Test case for {@link ImageLoader}.
 */
public class ImageLoaderTest extends AndroidTestCase implements ImageLoader.Callback {

    public static final String GOOGLE_LOGO = "https://www.google.com/images/srpr/logo3w.png";

    public static final String ERROR_IMAGE = "https://www.google.com/images/srpr/invalid.png";

    private CountDownLatch mImageLoaded;

    private CountDownLatch mImageError;

    /**
     * Checks if {@link ImageLoader.Callback#onImageLoaded(ImageView, String)} is called.
     */
    @LargeTest
    public void testLoadedCallback() throws InterruptedException {
        ImageLoader loader = new ImageLoader();
        ImageView view = new ImageView(getContext());

        // Load an image
        mImageLoaded = new CountDownLatch(1);
        assertEquals(BindResult.LOADING, loader.bind(view, GOOGLE_LOGO, this));
        mImageLoaded.await();

        // Load it again
        mImageLoaded = new CountDownLatch(1);
        assertEquals(BindResult.OK, loader.bind(view, GOOGLE_LOGO, this));

        // Check if callback was executed
        assertEquals(0, mImageLoaded.getCount());
    }

    /**
     * Checks if {@link ImageLoader.Callback#onImageError(ImageView, String, Throwable)} is called.
     */
    @LargeTest
    public void testErrorCallback() throws InterruptedException {
        ImageLoader loader = new ImageLoader();
        ImageView view = new ImageView(getContext());

        // Try to load an image that does not exist
        mImageError = new CountDownLatch(1);
        assertEquals(BindResult.LOADING, loader.bind(view, ERROR_IMAGE, this));
        mImageError.await();

        // Try to load it again
        mImageError = new CountDownLatch(1);
        assertEquals(BindResult.ERROR, loader.bind(view, ERROR_IMAGE, this));

        // Check if the callback was executed
        assertEquals(0, mImageError.getCount());
    }

    /**
     * Tries passing a {@code null} callback interface to
     * {@link ImageLoader#bind(ImageView, String, ImageLoader.Callback)}.
     */
    @LargeTest
    public void testNoCallback() {
        ImageLoader loader = new ImageLoader();
        ImageView view = new ImageView(getContext());
        assertEquals(BindResult.LOADING, loader.bind(view, GOOGLE_LOGO, null));
    }

    /**
     * {@inheritDoc}
     */
    public void onImageLoaded(ImageView view, String url) {
        mImageLoaded.countDown();
    }

    /**
     * {@inheritDoc}
     */
    public void onImageError(ImageView view, String url, Throwable error) {
        mImageError.countDown();
    }

}
