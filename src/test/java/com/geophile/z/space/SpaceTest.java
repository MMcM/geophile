/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.space;

import com.geophile.z.Space;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpaceTest
{
    @Test
    public void testBadDimensions()
    {
        // Too few dimensions
        try {
            new SpaceImpl(new int[]{}, null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        // Too many dimensions
        try {
            int[] dimensions = {5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
            assertTrue(dimensions.length > Space.MAX_DIMENSIONS);
            new SpaceImpl(dimensions, null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void interleaveVsZBits()
    {
        // 10 x bits but 11 space bits
        try {
            new SpaceImpl(new int[]{5, 5}, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0});
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void interleaveVsXBits()
    {
        // 5 bits for each coordinate, but that's not what the interleave specifies.
        try {
            new SpaceImpl(new int[]{5, 5}, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 1, 1});
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testDefaultInterleave()
    {
        SpaceImpl space = new SpaceImpl(new int[]{3, 5}, null);
        int[] interleave = space.interleave();
        assertEquals(0, interleave[0]);
        assertEquals(1, interleave[1]);
        assertEquals(0, interleave[2]);
        assertEquals(1, interleave[3]);
        assertEquals(0, interleave[4]);
        assertEquals(1, interleave[5]);
        assertEquals(1, interleave[6]);
        assertEquals(1, interleave[7]);
    }

    @Test
    public void test10()
    {
        SpaceImpl space = new SpaceImpl(ints(10), null);
        check(space, 0x0000000000000000L, longs(0x000));
        check(space, 0xffc0000000000000L, longs(0x3ff));
        check(space, 0xaa80000000000000L, longs(0x2aa));
        check(space, 0x5540000000000000L, longs(0x155));
    }

    @Test
    public void test10x10()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10), null);
        check(space, 0x0000000000000000L, longs(0x000, 0x000));
        check(space, 0x5555500000000000L, longs(0x000, 0x3ff));
        check(space, 0xaaaaa00000000000L, longs(0x3ff, 0x000));
        check(space, 0xfffff00000000000L, longs(0x3ff, 0x3ff));
        check(space, 0x1111100000000000L, longs(0x000, 0x155));
        check(space, 0x8888800000000000L, longs(0x2aa, 0x000));
        check(space, 0x9999900000000000L, longs(0x2aa, 0x155));
    }

    @Test
    public void test10x12()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 12), null);
        check(space, 0x0000000000000000L, longs(0x000, 0x000));
        check(space, 0x55555c0000000000L, longs(0x000, 0xfff));
        check(space, 0xaaaaa00000000000L, longs(0x3ff, 0x000));
        check(space, 0xfffffc0000000000L, longs(0x3ff, 0xfff));
        check(space, 0x1111140000000000L, longs(0x000, 0x555));
        check(space, 0x8888800000000000L, longs(0x2aa, 0x000));
        check(space, 0x9999940000000000L, longs(0x2aa, 0x555));
    }

    @Test
    public void test10x10x10()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10, 10), null);
        check(space, 0x0000000000000000L, longs(0x000, 0x000, 0x000));
        check(space, 0x2492492400000000L, longs(0x000, 0x000, 0x3ff));
        check(space, 0x4924924800000000L, longs(0x000, 0x3ff, 0x000));
        check(space, 0x6db6db6c00000000L, longs(0x000, 0x3ff, 0x3ff));
        check(space, 0x9249249000000000L, longs(0x3ff, 0x000, 0x000));
        check(space, 0xb6db6db400000000L, longs(0x3ff, 0x000, 0x3ff));
        check(space, 0xdb6db6d800000000L, longs(0x3ff, 0x3ff, 0x000));
        check(space, 0xfffffffc00000000L, longs(0x3ff, 0x3ff, 0x3ff));
        check(space, 0x0400400400000000L, longs(0x000, 0x000, 0x111));
        check(space, 0x4004004000000000L, longs(0x000, 0x222, 0x000));
        check(space, 0x4404404400000000L, longs(0x000, 0x222, 0x111));
        check(space, 0x9009009000000000L, longs(0x333, 0x000, 0x000));
        check(space, 0x9409409400000000L, longs(0x333, 0x000, 0x111));
        check(space, 0xd00d00d000000000L, longs(0x333, 0x222, 0x000));
        check(space, 0xd40d40d400000000L, longs(0x333, 0x222, 0x111));
    }

    @Test
    public void testSiblings()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10), null);
        final long Z_TEST = 0xaaaaaaaaaaaaaa80L;
        // Root not sibling of itself
        assertFalse(space.siblings(space.z(0, 0), space.z(0, 0)));
        // ... or of something it contains
        for (int bits = 0; bits < SpaceImpl.MAX_Z_BITS; bits++) {
            long base = prefix(Z_TEST, bits);
            long sibling = base ^ (1L << (64 - bits));
            // space-value isn't its own sibling
            assertFalse(space.siblings(space.z(base, bits), space.z(base, bits)));
            if (bits > 0) {
                // space-value is it's sibling's sibling
                assertTrue(space.siblings(space.z(base, bits), space.z(sibling, bits)));
                // siblings have to be at the same level
                assertFalse(space.siblings(space.z(base, bits), space.z(sibling, bits + 1)));
                // Flip an ancestor bit in sibling
                long notSibling = sibling ^ mask(bits / 2);
                assertFalse(space.siblings(space.z(base, bits), space.z(notSibling, bits)));
            }
        }
    }

    @Test
    public void testParent()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10), null);
        final long Z_TEST = 0xaaaaaaaaaaaaaa80L;
        for (int bits = 1; bits <= SpaceImpl.MAX_Z_BITS; bits++) {
            long z = space.z(prefix(Z_TEST, bits), bits);
            long parent = space.z(prefix(Z_TEST, bits - 1), bits - 1);
            assertEquals(parent, space.parent(z));

        }
    }

    @Test
    public void testContains()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10), null);
        final long Z_TEST = 0xaaaaaaaaaaaaaa80L;
        long z = space.z(Z_TEST, SpaceImpl.MAX_Z_BITS);
        for (int bits = 0; bits <= SpaceImpl.MAX_Z_BITS; bits++) {
            long ancestor = prefix(Z_TEST, bits);
            assertTrue(space.contains(space.z(ancestor, bits), z));
            if (bits > 0) {
                // Flip a bit in ancestor
                long notAncestor = ancestor ^ mask(bits / 2);
                assertFalse(space.contains(space.z(notAncestor, bits), z));
            }
        }
    }

    @Test
    public void testZLoZHi()
    {
        SpaceImpl space = new SpaceImpl(ints(10, 10), null);
        final long Z_TEST = 0xaaaaaaaaaaaaaa80L;
        for (int bits = 0; bits <= SpaceImpl.MAX_Z_BITS; bits++) {
            long expectedLo = prefix(Z_TEST, bits);
            long expectedHi = expectedLo | (((1L << (SpaceImpl.MAX_Z_BITS - bits))- 1) << (SpaceImpl.LENGTH_BITS + 1));
            assertEquals(space.z(expectedLo, bits), space.zLo(space.z(prefix(Z_TEST, bits), bits)));
            assertEquals(space.z(expectedHi, bits), space.zHi(space.z(prefix(Z_TEST, bits), bits)));
        }
    }

    private static int[] ints(int ... ints)
    {
        return ints;
    }

    private static long[] longs(long ... longs)
    {
        return longs;
    }

    private static long prefix(long x, int bits)
    {
        return x & ~(~0L >>> bits);
    }

    private static long mask(int position)
    {
        return 1L << (63 - position);
    }

    private void check(SpaceImpl space, long expected, long[] x)
    {
        assertEquals(space.z(expected, space.zBits()), space.shuffle(x));
    }
}
