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

import static android.test.MoreAsserts.assertContentsInAnyOrder;

import android.graphics.Bitmap;

import junit.framework.TestCase;

/**
 * Test case for {@link BitmapCache}.
 */
public class BitmapCacheTest extends TestCase {

    private static final Bitmap.Config CONFIG = Bitmap.Config.ARGB_8888;

    public void testLru() {
        long maxBytes = 512;
        int width = 8;
        int height = 8;
        BitmapCache<String> cache = new BitmapCache<String>(maxBytes);
        Bitmap bitmap1 = Bitmap.createBitmap(width, height, CONFIG);
        Bitmap bitmap2 = Bitmap.createBitmap(width, height, CONFIG);
        Bitmap bitmap3 = Bitmap.createBitmap(width, height, CONFIG);

        // Verify that cache fits exactly two bitmaps
        assertEquals(maxBytes, BitmapCache.sizeOf(bitmap1) * 2);

        cache.put("1", bitmap1);
        assertContentsInAnyOrder(cache.keySet(), "1");

        cache.put("2", bitmap2);
        assertContentsInAnyOrder(cache.keySet(), "1", "2");

        cache.put("3", bitmap3);
        assertContentsInAnyOrder(cache.keySet(), "2", "3");

        // Touch 2 so that 3 is evicted instead
        cache.get("2");
        cache.put("1", bitmap1);
        assertContentsInAnyOrder(cache.keySet(), "1", "2");
    }

    /**
     * Checks if multiple small bitmaps are evicted when a large bitmap is
     * inserted into the cache.
     */
    public void testMultipleEviction() {
        long maxSize = 256;
        BitmapCache<String> cache = new BitmapCache<String>(maxSize);
        Bitmap small1 = Bitmap.createBitmap(4, 4, CONFIG);
        Bitmap small2 = Bitmap.createBitmap(4, 4, CONFIG);
        Bitmap big1 = Bitmap.createBitmap(8, 8, CONFIG);

        // Check if the large bitmap is big enough to fill the cache
        assertEquals(maxSize, BitmapCache.sizeOf(big1));

        // Insert a couple small bitmaps
        cache.put("small1", small1);
        cache.put("small2", small2);
        assertContentsInAnyOrder(cache.keySet(), "small1", "small2");

        // Check if both small bitmaps are evicted by the big one
        cache.put("big1", big1);
        assertFalse(cache.containsKey("small1"));
        assertFalse(cache.containsKey("small2"));
        assertContentsInAnyOrder(cache.keySet(), "big1");
    }

}
