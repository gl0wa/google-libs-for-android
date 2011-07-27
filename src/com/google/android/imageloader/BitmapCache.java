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

import java.util.LinkedHashMap;
import java.util.Map;

import android.graphics.Bitmap;

/**
 * An LRU {@link Bitmap} cache.
 */
class BitmapCache<K> extends LinkedHashMap<K, Bitmap> {

    // Assume a 32-bit image
    private static final long BYTES_PER_PIXEL = 4;

    private static final int INITIAL_CAPACITY = 32;

    private static final float LOAD_FACTOR = 0.75f;

    private final long mMaxSize;

    /**
     * Constructor.
     *
     * @param maxSize the maximum size of the cache in bytes.
     */
    public BitmapCache(long maxSize) {
        super(INITIAL_CAPACITY, LOAD_FACTOR, true);
        mMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, Bitmap> eldest) {
        // This will increase the runtime for insertion to O(n),
        // but it's less error-prone than maintaining a counter.
        // The maximum value of n should generally be small as long
        // as the images are not tiny and the max size is not huge.
        return sizeOf(values()) > mMaxSize;

        // TODO: Remove additional elements if the cache is
        // still too big after removing the eldest entry.
    }

    private static long sizeOf(Bitmap b) {
        return b.getWidth() * b.getHeight() * BYTES_PER_PIXEL;
    }

    private static long sizeOf(Iterable<Bitmap> bitmaps) {
        long total = 0;
        for (Bitmap bitmap : bitmaps) {
            total += sizeOf(bitmap);
        }
        return total;
    }
}
