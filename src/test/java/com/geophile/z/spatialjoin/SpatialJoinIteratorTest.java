/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.Pair;
import com.geophile.z.SpatialJoin;
import com.geophile.z.spatialobject.d2.Box;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpatialJoinIteratorTest extends SpatialJoinIteratorTestBase
{
    @Test
    public void test() throws IOException, InterruptedException
    {
        TestInput leftInput = null;
        TestInput rightInput = null;
        for (int nLeft : COUNTS) {
            int nRight = MAX_COUNT / nLeft;
            assertEquals(MAX_COUNT, nLeft * nRight);
            for (int maxLeftXSize : MAX_SIZES) {
                for (int maxLeftYSize : MAX_SIZES) {
                    for (int maxRightXSize : MAX_SIZES) {
                        for (int maxRightYSize : MAX_SIZES) {
                            for (int trial = 0; trial < TRIALS; trial++) {
                                if (trial == 0 || nLeft < nRight) {
                                    leftInput = loadBoxes(nLeft, maxLeftXSize, maxLeftYSize);
                                }
                                if (trial == 0 || nRight <= nLeft) {
                                    rightInput = loadBoxes(nRight, maxRightXSize, maxRightYSize);
                                }
                                test(leftInput, rightInput, SpatialJoin.Duplicates.INCLUDE);
                                test(leftInput, rightInput, SpatialJoin.Duplicates.EXCLUDE);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void selfJoin() throws IOException, InterruptedException
    {
        SpatialJoinFilter<Box, Box> filter =
            new SpatialJoinFilter<Box, Box>()
            {
                @Override
                public boolean overlap(Box a, Box b)
                {
                    return a.equalTo(b);
                }
            };
        for (int maxXSize : MAX_SIZES) {
            for (int maxYSize : MAX_SIZES) {
                TestInput input = loadBoxes(10_000, maxXSize, maxYSize);
                Set<Box> actual = new HashSet<>();
                Iterator<Pair<Box, Box>> iterator =
                    SpatialJoin.newSpatialJoin(filter, SpatialJoin.Duplicates.EXCLUDE)
                               .iterator(input.spatialIndex(), input.spatialIndex());
                while (iterator.hasNext()) {
                    Pair<Box, Box> pair = iterator.next();
                    Box box = pair.left();
                    assertTrue(box.equalTo(pair.right()));
                    actual.add(box);
                }
                assertEquals(new HashSet<>(input.boxes()), actual);
            }
        }
    }

    @Override
    protected Box randomBox(int maxXSize, int maxYSize)
    {
        long xLo = random.nextInt(NX - maxXSize);
        long xHi = xLo + (maxXSize == 1 ? 0 : random.nextInt(maxXSize));
        long yLo = random.nextInt(NY - maxYSize);
        long yHi = yLo + (maxYSize == 1 ? 0 : random.nextInt(maxYSize));
        return new Box(xLo, xHi, yLo, yHi);
    }

    @Override
    protected void checkEquals(Object expected, Object actual)
    {
        assertEquals(expected, actual);
    }

    @Override
    protected boolean verify()
    {
        return true;
    }

    protected static final int MAX_COUNT = 100_000; // 1_000_000;
    protected static final int[] COUNTS = new int[]{1, 10, 100, 1_000, 10_000, 100_000 /*, 1_000_000 */};
    protected static final int[] MAX_SIZES = new int[]{1, 10_000, /* 1% */ 100_000 /* 10% */};
}
