/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.index;

import com.geophile.spatialobject.d2.Point;
import com.geophile.z.SpaceImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static junit.framework.Assert.*;

public class CursorTest
{
    @Test
    public void testCursor() throws IOException, InterruptedException
    {
        for (int n = 0; n <= N_MAX; n++) {
            testCursor(testIndex(n), n);
        }
    }

    private void testCursor(Index<Point> index, int n) throws IOException, InterruptedException
    {
        Cursor<Point> cursor;
        int expectedKey;
        int expectedLastKey;
        boolean expectedEmpty;
        Record<Point> entry;
        // Full cursor
        {
            cursor = index.cursor(0L);
            expectedKey = 0;
            while ((entry = cursor.next()) != null) {
                assertEquals(expectedKey, key(entry));
                expectedKey += GAP;
            }
            assertEquals(n * GAP, expectedKey);
        }
        // Try scans starting at, before, and after each key and ending at, before and after each key.
        {
            for (int i = 0; i < n; i++) {
                int startBase = GAP * i;
                int endBase = GAP * (n - 1 - i);
                for (long start = startBase - 1; start <= startBase + 1; start++) {
                    for (long end = endBase - 1; end <= endBase + 1; end++) {
                        if (start <= end) {
                            // debug("start: %s, end: %s", start, end);
                            cursor = index.cursor(start);
                            expectedKey = start <= startBase ? startBase : startBase + GAP;
                            expectedLastKey = end >= endBase ? endBase : endBase - GAP;
                            expectedEmpty = start > end || start <= end && (end >= startBase || start <= endBase);
                            boolean empty = true;
                            while ((entry = cursor.next()) != null &&
                                   entry.getKey().compareTo(end) <= 0) {
                                // debug("    %s", entry.getKey());
                                assertEquals(expectedKey, key(entry));
                                expectedKey += GAP;
                                empty = false;
                            }
                            if (empty) {
                                assertTrue(expectedEmpty);
                            } else {
                                assertEquals(expectedLastKey + GAP, expectedKey);
                            }
                        }
                    }
                }
            }
        }
        // Alternating next and previous
        {
            // debug("n: %s", n);
            cursor = index.cursor(0L);
            expectedKey = 0;
            entry = cursor.next();
            if (entry != null) {
                // debug("expected: %s, start: %s", expectedKey, entry.getKey());
                expectedKey += GAP;
            }
            while ((entry = cursor.next()) != null) {
                // debug("expected: %s, next: %s", expectedKey, entry.getKey());
                assertEquals(expectedKey, key(entry));
                expectedKey += GAP;
                if (expectedKey != n * GAP) {
                    entry = cursor.next();
                    // debug("expected: %s, next: %s", expectedKey, entry.getKey());
                    assertNotNull(entry);
                    assertEquals(expectedKey, key(entry));
                    expectedKey -= GAP;
                    entry = cursor.previous();
                    // debug("expected: %s, previous: %s", expectedKey, entry.getKey());
                    assertEquals(expectedKey, key(entry));
                    expectedKey += GAP; // About to go to next
                }
            }
            assertEquals(n * GAP, expectedKey);
        }
        // Alternating previous and next
        {
            // debug("n: %s", n);
            cursor = index.cursor(Long.MAX_VALUE);
            expectedKey = (n - 1) * GAP;
            entry = cursor.previous();
            if (entry != null) {
                // debug("expected: %s, start: %s", expectedKey, entry.getKey());
                expectedKey -= GAP;
            }
            while ((entry = cursor.previous()) != null) {
                // debug("expected: %s, previous: %s", expectedKey, entry.getKey());
                assertEquals(expectedKey, key(entry));
                expectedKey -= GAP;
                if (expectedKey >= 0) {
                    entry = cursor.previous();
                    // debug("expected: %s, previous: %s", expectedKey, entry.getKey());
                    assertNotNull(entry);
                    assertEquals(expectedKey, key(entry));
                    expectedKey += GAP;
                    entry = cursor.next();
                    // debug("expected: %s, next: %s", expectedKey, entry.getKey());
                    assertEquals(expectedKey, key(entry));
                    expectedKey -= GAP; // About to go to next
                }
            }
            assertEquals(-GAP, expectedKey);
        }
        // goTo
        if (n > 0) {
            cursor = index.cursor(0L);
            int match;
            int before;
            for (int i = 0; i <= n; i++) {
                // debug("n: %s, i: %s", n, i);
                match = i * GAP;
                if (i < n) {
                    // Match, next
                    cursor.goTo(match);
                    assertEquals(match, key(cursor.next()));
                    // Match, previous
                    cursor.goTo(match);
                    assertEquals(match, key(cursor.previous()));
                }
                // Before, next
                before = match - GAP / 2;
                cursor.goTo(before);
                if (i == n) {
                    assertNull(cursor.next());
                } else {
                    assertEquals(match, key(cursor.next()));
                }
                // Before, previous
                cursor.goTo(before);
                if (i == 0) {
                    assertNull(cursor.previous());
                } else {
                    assertEquals(match - GAP, key(cursor.previous()));
                }
            }
        }
    }

    private Index<Point> testIndex(int n) throws IOException
    {
        SpaceImpl space = new SpaceImpl(new int[]{10, 10}, null);
        Index<Point> index = new TreeIndex<>(space);
        assertTrue(GAP > 1);
        // Populate map with keys 0, GAP, ..., GAP * (n - 1)
        for (int i = 0; i < n; i++) {
            long key = GAP * i;
            index.add(key, new Point(key, key));
        }
        return index;
    }

    private long key(Record<Point> entry)
    {
        return entry.getKey();
    }

    private void debug(String template, Object ... args)
    {
        System.out.println(String.format(template, args));
    }

    private static final int N_MAX = 100;
    private static final int GAP = 10;
}
