/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// Spatial join of inputs containing boxes, varying for each input:
// - counts
// - box aspect ratio
// - duplicate handling
// This is the main test of spatial join logic, and is not concerned with variations in
// indexes and spatial objects.

public class SpatialJoinTest extends SpatialJoinTestBase
{
    @Test
    public void testSpaceMismatch() throws IOException, InterruptedException
    {
        // Dimensions mismatch
        {
            SpatialIndex<TestRecord> leftSpatialIndex =
                SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0, 0},
                                                            new double[]{1000, 1000, 1000},
                                                            new int[]{10, 10, 10}),
                                             newIndex());
            SpatialIndex<TestRecord> rightSpatialIndex =
                SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0},
                                                            new double[]{1000, 1000},
                                                            new int[]{10, 10}),
                                             newIndex());
            try {
                SpatialJoin
                    .iterator(leftSpatialIndex, rightSpatialIndex, MANY_MANY_FILTER, SpatialJoin.Duplicates.EXCLUDE);
                fail();
            } catch (SpatialJoinException e) {
                // Expected
            }
        }
        // Space bounds mismatch
        {
            SpatialIndex<TestRecord> leftSpatialIndex =
                SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0},
                                                            new double[]{1000, 1000},
                                                            new int[]{10, 10}),
                                             newIndex());
            SpatialIndex<TestRecord> rightSpatialIndex =
                SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0},
                                                            new double[]{2000, 1000},
                                                            new int[]{10, 10}),
                                             newIndex());
            try {
                SpatialJoin
                    .iterator(leftSpatialIndex, rightSpatialIndex, MANY_MANY_FILTER, SpatialJoin.Duplicates.EXCLUDE);
                fail();
            } catch (SpatialJoinException e) {
                // Expected
            }
        }
    }

    @Test
    public void testNonIdenticalSpaceMatch() throws IOException, InterruptedException
    {
        SpatialIndex<TestRecord> leftSpatialIndex =
            SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0},
                                                        new double[]{1000, 1000},
                                                        new int[]{10, 10}),
                                         newIndex());
        SpatialIndex<TestRecord> rightSpatialIndex =
            SpatialIndex.newSpatialIndex(Space.newSpace(new double[]{0, 0},
                                                        new double[]{1000, 1000},
                                                        new int[]{10, 10}),
                                         newIndex());
        SpatialJoin.iterator(leftSpatialIndex, rightSpatialIndex, MANY_MANY_FILTER, SpatialJoin.Duplicates.EXCLUDE);
    }

    @Test
    public void testSpatialJoin() throws IOException, InterruptedException
    {
        TestInput leftInput = null;
        TestInput rightInput = null;
        int testCount = 0;
        for (int nLeft : COUNTS) {
            int nRight = MAX_COUNT / nLeft;
            assertEquals(MAX_COUNT, nLeft * nRight);
            for (int maxLeftXSize : MAX_SIZES) {
                for (int maxLeftYSize : MAX_SIZES) {
                    BoxGenerator leftBoxGenerator =
                        new BoxGenerator(SPACE, random, maxLeftXSize, maxLeftYSize);
                    for (int maxRightXSize : MAX_SIZES) {
                        for (int maxRightYSize : MAX_SIZES) {
                            BoxGenerator rightBoxGenerator =
                                new BoxGenerator(SPACE, random, maxRightXSize, maxRightYSize);
                            for (int trial = 0; trial < TRIALS; trial++) {
                                if (trial == 0 || nLeft < nRight) {
                                    leftInput = newTestInput(nLeft, leftBoxGenerator);
                                }
                                if (trial == 0 || nRight <= nLeft) {
                                    rightInput = newTestInput(nRight, rightBoxGenerator);
                                }
                                testJoin(leftInput,
                                         rightInput,
                                         MANY_MANY_FILTER,
                                         ONE_MANY_FILTER,
                                         SpatialJoin.Duplicates.INCLUDE);
                                testJoin(leftInput,
                                         rightInput,
                                         MANY_MANY_FILTER,
                                         ONE_MANY_FILTER,
                                         SpatialJoin.Duplicates.EXCLUDE);
                                testCount++;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Space space()
    {
        return SPACE;
    }

    @Override
    protected Index<TestRecord> newIndex()
    {
        return new TestIndex();
    }

    @Override
    protected boolean overlap(SpatialObject s, SpatialObject t)
    {
        return OVERLAP_TESTER.overlap(s, t);
    }

    @Override
    protected boolean verify()
    {
        return true;
    }

    @Override
    protected boolean printSummary()
    {
        return false;
    }

    @Override
    protected boolean trace()
    {
        return false;
    }

    private TestInput newTestInput(int n, BoxGenerator boxGenerator) throws IOException, InterruptedException
    {
        Index<TestRecord> index = newIndex();
        SpatialIndex<TestRecord> spatialIndex = SpatialIndex.newSpatialIndex(SPACE, index);
        TestInput testInput = new TestInput(spatialIndex, boxGenerator.description());
        load(n, boxGenerator, testInput);
        return testInput;
    }

    private static final int MAX_COUNT = 100_000;
    private static final int[] COUNTS = new int[]{1, 10, 100, 1_000, 10_000};
    private static final int[] MAX_SIZES = new int[]{1, 10_000, /* 1% */ 100_000 /* 10% */};
    private static final int NX = 1_000_000;
    private static final int NY = 1_000_000;
    private static final int X_BITS = 20;
    private static final int Y_BITS = 20;
    private static final int TRIALS = 1;
    private static final BoxOverlapTester OVERLAP_TESTER = new BoxOverlapTester();
    private static final Space SPACE = Space.newSpace(new double[]{0, 0},
                                                      new double[]{NX, NY},
                                                      new int[]{X_BITS, Y_BITS});

    private final Random random = new Random(123456);
    private final SpatialJoinFilter<TestRecord, TestRecord> MANY_MANY_FILTER =
        new SpatialJoinFilter<TestRecord, TestRecord>()
        {
            @Override
            public boolean overlap(TestRecord left, TestRecord right)
            {
                testStats.filterCount++;
                boolean overlap = OVERLAP_TESTER.overlap(left.spatialObject(), right.spatialObject());
                if (overlap) {
                    testStats.overlapCount++;
                }
                return overlap;
            }
        };
    private final SpatialJoinFilter<SpatialObject, TestRecord> ONE_MANY_FILTER =
        new SpatialJoinFilter<SpatialObject, TestRecord>()
        {
            @Override
            public boolean overlap(SpatialObject left, TestRecord right)
            {
                testStats.filterCount++;
                boolean overlap = OVERLAP_TESTER.overlap(left, right.spatialObject());
                if (overlap) {
                    testStats.overlapCount++;
                }
                return overlap;
            }
        };
}
