package com.geophile.z.spatialjoin;

import com.geophile.z.Pair;
import com.geophile.z.Record;
import com.geophile.z.SpatialJoinFilter;
import com.geophile.z.SpatialJoinRuntimeException;
import com.geophile.z.space.SpatialIndexImpl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

// T is either Pair or SpatialObject

class SpatialJoinIterator<T> implements Iterator<T>
{
    // Object interface

    @Override
    public String toString()
    {
        return name;
    }

    // Iterator interface

    @Override
    public boolean hasNext()
    {
        ensurePending();
        return !pending.isEmpty();
    }

    @Override
    public T next()
    {
        T next;
        ensurePending();
        if (pending.isEmpty()) {
            throw new NoSuchElementException();
        }
        next = pending.poll();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.INFO, "{0} -> {1}", new Object[]{this, next});
        }
        return next;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }


    // SpatialJoinIterator interface

    public static SpatialJoinIterator<Pair> pairIterator(SpatialIndexImpl leftSpatialIndex,
                                                         SpatialIndexImpl rightSpatialIndex,
                                                         SpatialJoinFilter filter)
        throws IOException, InterruptedException
    {
        return new SpatialJoinIterator<>(leftSpatialIndex,
                                         rightSpatialIndex,
                                         PAIR_OUTPUT_GENERATOR,
                                         filter);
    }

    public static SpatialJoinIterator<Record> spatialObjectIterator(SpatialIndexImpl leftSpatialIndex,
                                                                    SpatialIndexImpl rightSpatialIndex,
                                                                    SpatialJoinFilter filter)
        throws IOException, InterruptedException
    {
        return new SpatialJoinIterator<>(leftSpatialIndex,
                                         rightSpatialIndex,
                                         RECORD_OUTPUT_GENERATOR,
                                         filter);
    }

    // For use by this class

    private SpatialJoinIterator(SpatialIndexImpl leftSpatialIndex,
                                SpatialIndexImpl rightSpatialIndex,
                                final OutputGenerator<T> outputGenerator,
                                final SpatialJoinFilter filter) throws IOException, InterruptedException
    {
        SpatialJoinOutput pendingLeftRight =
            new SpatialJoinOutput()
            {
                @Override
                public void add(Record left, Record right)
                {
                    if (filter.overlap(left, right)) {
                        pending.add(outputGenerator.generateOutput(left, right));
                    }
                }
            };
        left = SpatialJoinInput.newSpatialJoinInput(leftSpatialIndex, pendingLeftRight);
        SpatialJoinOutput pendingRightLeft =
            new SpatialJoinOutput()
            {
                @Override
                public void add(Record right, Record left)
                {
                    if (filter.overlap(left, right)) {
                        pending.add(outputGenerator.generateOutput(left, right));
                    }
                }
            };
        right = SpatialJoinInput.newSpatialJoinInput(rightSpatialIndex, pendingRightLeft);
        left.otherInput(right);
        right.otherInput(left);
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO,
                    "SpatialJoinIterator {0}: {1} x {2}",
                    new Object[]{this, left, right});
        }
        findPairs();
    }

    private void ensurePending()
    {
        if (pending.isEmpty()) {
            try {
                findPairs();
            } catch (IOException | InterruptedException e) {
                throw new SpatialJoinRuntimeException(e);
            }
        }
    }

    private void findPairs() throws IOException, InterruptedException
    {
        assert pending.isEmpty();
        long zMin;
        do {
            long zLeftEnter = left.nextEntry();
            long zLeftExit = left.nextExit();
            long zRightEnter = right.nextEntry();
            long zRightExit = right.nextExit();
            zMin = min(zLeftEnter, zLeftExit, zRightEnter, zRightExit);
            if (zMin < SpatialJoinInput.EOF) {
                // Prefer entry to exit to avoid missing join output
                if (zMin == zLeftEnter) {
                    left.enterZ();
                } else if (zMin == zRightEnter) {
                    right.enterZ();
                } else if (zMin == zLeftExit) {
                    left.exitZ();
                } else {
                    right.exitZ();
                }
            }
        } while (pending.isEmpty() && zMin < SpatialJoinInput.EOF);
    }

    private long min(long a, long b, long c, long d)
    {
        long minAB = a < b ? a : b;
        long minCD = c < d ? c : d;
        return minAB < minCD ? minAB : minCD;
    }

    // Class state

    private static final Logger LOG = Logger.getLogger(SpatialJoinIterator.class.getName());
    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private static final OutputGenerator<Pair> PAIR_OUTPUT_GENERATOR =
        new PairOutputGenerator();
    private static final OutputGenerator<Record> RECORD_OUTPUT_GENERATOR =
        new RecordOutputGenerator();

    // Object state

    private final String name = String.format("sj(%s)", idGenerator.getAndIncrement());
    private final SpatialJoinInput left;
    private final SpatialJoinInput right;
    private final Queue<T> pending = new ArrayDeque<>();

    // Inner classes

    private interface OutputGenerator<T>
    {
        T generateOutput(Record left, Record right);
    }

    private static class PairOutputGenerator implements OutputGenerator<Pair>
    {
        @Override
        public Pair generateOutput(Record left, Record right)
        {
            return new Pair(left, right);
        }
    }

    private static class RecordOutputGenerator implements OutputGenerator<Record>
    {
        @Override
        public Record generateOutput(Record left, Record right)
        {
            return right;
        }
    }
}
