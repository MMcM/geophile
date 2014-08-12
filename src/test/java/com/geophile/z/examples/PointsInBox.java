/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.examples;

import com.geophile.z.*;
import com.geophile.z.SpatialJoinFilter;
import com.geophile.z.spatialjoin.SpatialJoinImpl;
import com.geophile.z.spatialobject.d2.Box;
import com.geophile.z.spatialobject.d2.Point;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

public class PointsInBox
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        new PointsInBox().run();
    }

    private void run() throws IOException, InterruptedException
    {
        // Load spatial index with points
        SpatialIndex<Record> points = SpatialIndex.newSpatialIndex(SPACE, new TestIndex());
        for (int i = 0; i < N_POINTS; i++) {
            points.add(new Record(randomPoint(), i));
        }
        // Run queries
        for (int q = 0; q < N_QUERIES; q++) {
            // Create Iterator over spatial join output
            Box box = randomBox();
            Iterator<Record> iterator =
                SpatialJoin.newSpatialJoin(BOX_CONTAINS_POINT, SpatialJoinImpl.Duplicates.EXCLUDE)
                           .iterator(box, points);
            // Print points contained in box
            System.out.println(String.format("Points inside %s", box));
            while (iterator.hasNext()) {
                com.geophile.z.Record record = iterator.next();
                System.out.println(String.format("    %s", record.spatialObject()));
            }
        }
    }

    private Point randomPoint()
    {
        int x = random.nextInt(X);
        int y = random.nextInt(Y);
        return new Point(x, y);
    }

    private Box randomBox()
    {
        int xLo = random.nextInt(X - BOX_WIDTH);
        int xHi = xLo + BOX_WIDTH - 1;
        int yLo = random.nextInt(Y - BOX_HEIGHT);
        int yHi = yLo + BOX_HEIGHT - 1;
        return new Box(xLo, xHi, yLo, yHi);
    }

    private static final int X = 1_000_000;
    private static final int Y = 1_000_000;
    private static final int X_BITS = 20;
    private static final int Y_BITS = 20;
    private static final int N_POINTS = 1_000_000;
    private static final int BOX_WIDTH = 2_000;
    private static final int BOX_HEIGHT = 2_000;
    private static final int N_QUERIES = 5;
    private static final Space SPACE = Space.newSpace(new double[]{0, 0},
                                                      new double[]{X, Y},
                                                      new int[]{X_BITS, Y_BITS});
    private static final SpatialJoinFilter BOX_CONTAINS_POINT =
        new SpatialJoinFilter()
        {
            @Override
            public boolean overlap(SpatialObject r, SpatialObject s)
            {
                Box box = (Box) r;
                Point point = (Point) s;
                return
                    box.xLo() <= point.x() && point.x() <= box.xHi() &&
                    box.yLo() <= point.y() && point.y() <= box.yHi();
            }
        };

    private final Random random = new Random(System.currentTimeMillis());
}
