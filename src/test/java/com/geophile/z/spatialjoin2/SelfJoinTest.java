/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin2;

import com.geophile.z.*;
import com.geophile.z.index.treeindex.TreeIndex;
import com.geophile.z.spatialjoin.SpatialJoinFilter;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

public class SelfJoinTest extends SpatialJoinTestBase
{
    @Test
    public void selfJoin() throws IOException, InterruptedException
    {
        SpatialJoinFilter filter =
            new SpatialJoinFilter()
            {
                @Override
                public boolean overlap(SpatialObject s, SpatialObject t)
                {
                    testStats.filterCount++;
                    boolean overlap = OVERLAP_TESTER.overlap(s, t);
                    if (overlap) {
                        testStats.overlapCount++;
                    }
                    return overlap;
                }
            };
        SpatialJoin spatialJoin = SpatialJoin.newSpatialJoin(filter, SpatialJoin.Duplicates.EXCLUDE);
        for (int maxXSize : MAX_SIZES) {
            for (int maxYSize : MAX_SIZES) {
                BoxGenerator boxGenerator = new BoxGenerator(APP_SPACE, random, maxXSize, maxYSize);
                TestInput input = newTestInput(COUNT, boxGenerator);
                testJoin(spatialJoin, input, input);
            }
        }
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
        return true;
    }

    @Override
    protected boolean trace()
    {
        return false;
    }

    private TestInput newTestInput(int n, BoxGenerator boxGenerator) throws IOException, InterruptedException
    {
        Index index = new TreeIndex();
        SpatialIndex spatialIndex = SpatialIndex.newSpatialIndex(SPACE, index);
        TestInput testInput = new TestInput(spatialIndex, boxGenerator.description());
        load(n, boxGenerator, testInput);
        return testInput;
    }

    private static final int NX = 1_000_000;
    private static final int NY = 1_000_000;
    private static final int LOG_NX = 20;
    private static final int LOG_NY = 20;
    private static final ApplicationSpace APP_SPACE = appSpace(0, NX, 0, NY);
    private static final Space SPACE = Space.newSpace(APP_SPACE, LOG_NX, LOG_NY);
    private static final int COUNT = 10_000;
    private static final int[] MAX_SIZES = new int[]{1, 10_000, /* 1% */ 100_000 /* 10% */};
    private static final BoxOverlapTester OVERLAP_TESTER = new BoxOverlapTester();
    private final Random random = new Random(654321);
}
