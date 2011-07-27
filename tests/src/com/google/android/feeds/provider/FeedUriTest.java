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

package com.google.android.feeds.provider;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test case for {@link FeedUri}.
 */
public class FeedUriTest extends TestCase {

    private static final List<String> list(String... elements) {
        return Arrays.asList(elements);
    }

    public void testReplaceQueryParameter() {
        Uri input = Uri.parse("http://example.com?a=1&b=%25&c=%E6%88%91");
        Uri output = FeedUri.replaceQueryParameter(input, "a", "2");

        // Check the parts individually because the order
        // of parameters in the output is undefined.
        assertEquals("http", output.getScheme());
        assertEquals("example.com", output.getAuthority());
        assertEquals(list("2"), output.getQueryParameters("a"));
        assertEquals(list("%"), output.getQueryParameters("b"));
        assertEquals(list("\u6211"), output.getQueryParameters("c"));
    }
}
