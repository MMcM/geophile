/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.space;

import com.geophile.z.*;
import com.geophile.z.spatialjoin.SpatialJoinFilter;
import com.geophile.z.spatialjoin.SpatialJoinImpl;
import com.geophile.z.spatialobject.d2.Box;
import com.geophile.z.spatialobject.d2.Point;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public abstract class SpatialIndexTestBase
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        SERIALIZER.register(1, Point.class);
        SERIALIZER.register(2, Box.class);
    }

    // Like TreeIndexTest.testRetrieval, but written in terms of SpatialIndex
    @Test
    public void testRetrieval() throws Exception
    {
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        for (long x = 0; x < X_MAX; x += 10) {
            for (long y = 0; y < Y_MAX; y += 10) {
                spatialIndex.add(new Point(x, y));
            }
        }
        commitTransaction();
        Random random = new Random(SEED);
        for (int i = 0; i < 1000; i++) {
            generateRandomBox(random);
            test(spatialIndex,
                 xLo, xHi, yLo, yHi,
                 new Filter()
                 {
                     @Override
                     public boolean keep(SpatialObject spatialObject)
                     {
                         return true;
                     }
                 });
        }
    }

    @Test
    public void testRemoveAll() throws Exception
    {
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        for (long x = 0; x < X_MAX; x += 10) {
            for (long y = 0; y < Y_MAX; y += 10) {
                spatialIndex.add(new Point(x, y));
            }
        }
        commitTransaction();
        // Remove everything
        RemovalFilter removalFilter = new RemovalFilter();
        for (long x = 0; x < X_MAX; x += 10) {
            for (long y = 0; y < Y_MAX; y += 10) {
                Point point = new Point(x, y);
                removalFilter.spatialObject(point);
                spatialIndex.remove(point, removalFilter);
            }
        }
        commitTransaction();
        Random random = new Random(SEED);
        for (int i = 0; i < 1000; i++) {
            generateRandomBox(random);
            test(spatialIndex,
                 xLo, xHi, yLo, yHi,
                 new Filter()
                 {
                     @Override
                     public boolean keep(SpatialObject spatialObject)
                     {
                         return false;
                     }
                 });
        }
    }

    @Test
    public void testRemoveSome() throws Exception
    {
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        for (long x = 0; x < X_MAX; x += 10) {
            for (long y = 0; y < Y_MAX; y += 10) {
                spatialIndex.add(new Point(x, y));
            }
        }
        commitTransaction();
        // Remove (x, y), for odd x/10 and even y/10
        RemovalFilter removalFilter = new RemovalFilter();
        for (long x = 0; x < X_MAX; x += 10) {
            if ((x / 10) % 2 == 1) {
                for (long y = 0; y < Y_MAX; y += 10) {
                    if ((y / 10) % 2 == 0) {
                        Point point = new Point(x, y);
                        removalFilter.spatialObject(point);
                        spatialIndex.remove(point, removalFilter);
                    }
                }
            }
        }
        commitTransaction();
        Random random = new Random(SEED);
        int xLo;
        int xHi;
        int yLo;
        int yHi;
        for (int i = 0; i < 1000; i++) {
            do {
                xLo = random.nextInt(X_MAX);
                xHi = xLo + random.nextInt(X_MAX - xLo);
            } while (xHi <= xLo);
            do {
                yLo = random.nextInt(Y_MAX);
                yHi = random.nextInt(Y_MAX - yLo);
            } while (yHi <= yLo);
            test(spatialIndex,
                 xLo, xHi, yLo, yHi,
                 new Filter()
                 {
                     @Override
                     public boolean keep(SpatialObject spatialObject)
                     {
                         Point point = (Point) spatialObject;
                         return !((point.x() / 10) % 2 == 1 && (point.y() / 10) % 2 == 0);
                     }
                 });
        }
    }

    @Test
    public void testRemovalVsDuplicates() throws Exception
    {
        final int COPIES = 10;
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        Box box = new Box(250, 750, 250, 750);
        for (int c = 0; c < COPIES; c++) {
            spatialIndex.add(box);
        }
        commitTransaction();
    }

    @Test
    public void spatialIdGeneratorRestore() throws Exception
    {
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        // pX: expected id is X
        Point p0 = new Point(0, 0);
        spatialIndex.add(p0);
        assertEquals(0, p0.id());
        Point p1 = new Point(1, 1);
        spatialIndex.add(p1);
        assertEquals(1, p1.id());
        Point p2 = new Point(2, 2);
        spatialIndex.add(p2);
        assertEquals(2, p2.id());
        // Creating a new SpatialIndexImpl restores the id generator
        spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        Point q0 = new Point(0, 0);
        spatialIndex.add(q0);
        assertEquals(SpatialIndexImpl.soidReservationBlockSize() + 0, q0.id());
        Point q1 = new Point(1, 1);
        spatialIndex.add(q1);
        assertEquals(SpatialIndexImpl.soidReservationBlockSize() + 1, q1.id());
        Point q2 = new Point(2, 2);
        spatialIndex.add(q2);
        assertEquals(SpatialIndexImpl.soidReservationBlockSize() + 2, q2.id());
    }

    @Test
    public void spatialIdGeneratorTracking() throws Exception
    {
        final int SOID_RESERVATION_BLOCK_SIZE = 3;
        System.setProperty(SpatialIndexImpl.SOID_RESERVALTION_BLOCK_SIZE_PROPERTY,
                           Integer.toString(SOID_RESERVATION_BLOCK_SIZE));
        Index index = newIndex();
        SpatialIndexImpl spatialIndex = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        assertEquals(SOID_RESERVATION_BLOCK_SIZE, spatialIndex.firstUnreservedSoid());
        assertEquals(SOID_RESERVATION_BLOCK_SIZE, spatialIndex.firstUnreservedSoidStored());
        spatialIndex.add(new Point(0, 0));
        spatialIndex.add(new Point(1, 1));
        spatialIndex.add(new Point(2, 2));
        assertEquals(SOID_RESERVATION_BLOCK_SIZE, spatialIndex.firstUnreservedSoid());
        assertEquals(SOID_RESERVATION_BLOCK_SIZE, spatialIndex.firstUnreservedSoidStored());
        spatialIndex.add(new Point(3, 3));
        assertEquals(SOID_RESERVATION_BLOCK_SIZE * 2, spatialIndex.firstUnreservedSoid());
        assertEquals(SOID_RESERVATION_BLOCK_SIZE * 2, spatialIndex.firstUnreservedSoidStored());
    }

    public abstract Index newIndex() throws Exception;

    public void commitTransaction() throws Exception
    {}

    private void test(SpatialIndexImpl spatialIndex,
                      int xLo, int xHi, int yLo, int yHi,
                      Filter filter) throws Exception
    {
        Box box = new Box(xLo, xHi, yLo, yHi);
        Index index = newIndex();
        SpatialIndex query = new SpatialIndexImpl(SPACE, index, SpatialIndex.Options.DEFAULT);
        query.add(box);
        Iterator<Pair> iterator =
            SpatialJoin.newSpatialJoin(FILTER, SpatialJoinImpl.Duplicates.INCLUDE).iterator(query, spatialIndex);
        List<Point> actual = new ArrayList<>();
        while (iterator.hasNext()) {
            Point point = (Point) iterator.next().right();
            if (!actual.contains(point)) {
                actual.add(point);
            }
        }
        List<Point> expected = new ArrayList<>();
        for (long x = 10 * ((xLo + 9) / 10); x <= 10 * (xHi / 10); x += 10) {
            for (long y = 10 * ((yLo + 9) / 10); y <= 10 * (yHi / 10); y += 10) {
                Point point = new Point(x, y);
                if (filter.keep(point)) {
                    expected.add(point);
                }
            }
        }
        clearIds(actual);
        clearIds(expected);
        Collections.sort(actual, POINT_RANKING);
        Collections.sort(expected, POINT_RANKING);
        assertEquals(expected, actual);
    }

    private void generateRandomBox(Random random)
    {
        do {
            xLo = random.nextInt(X_MAX);
            xHi = xLo + random.nextInt(X_MAX - xLo);
        } while (xHi <= xLo);
        do {
            yLo = random.nextInt(Y_MAX);
            yHi = yLo + random.nextInt(Y_MAX - yLo);
        } while (yHi <= yLo);
    }

    private void clearIds(Collection<Point> points)
    {
        for (Point point : points) {
            point.id(0);
        }
    }

    private static final int SEED = 123456;
    private static final int X_MAX = 1000;
    private static final int Y_MAX = 1000;
    private static final Comparator<Point> POINT_RANKING =
        new Comparator<Point>()
        {
            @Override
            public int compare(Point p, Point q)
            {
                int c = p.x() < q.x() ? -1 : p.x() > q.x() ? 1 : 0;
                if (c == 0) {
                    c = p.y() < q.y() ? -1 : p.y() > q.y() ? 1 : 0;
                }
                return c;
            }
        };
    private static final SpaceImpl SPACE = new SpaceImpl(new double[]{0, 0}, new double[]{1000, 1000}, new int[]{10, 10}, null);
    private static final SpatialJoinFilter FILTER = new SpatialJoinFilter()
    {
        @Override
        public boolean overlap(SpatialObject x, SpatialObject y)
        {
            Box b = (Box) x;
            Point p = (Point) y;
            return
                b.xLo() <= p.x() && p.x() <= b.xHi() &&
                b.yLo() <= p.y() && p.y() <= b.yHi();
        }
    };
    protected static final Serializer SERIALIZER = Serializer.newSerializer();

    private static interface Filter
    {
        boolean keep(SpatialObject spatialObject);
    }

    private int xLo;
    private int xHi;
    private int yLo;
    private int yHi;
}
