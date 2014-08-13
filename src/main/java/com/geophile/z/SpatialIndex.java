/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z;

import com.geophile.z.space.SpaceImpl;
import com.geophile.z.space.SpatialIndexImpl;

import java.io.IOException;

/**
 * A SpatialIndex organizes a set of {@link SpatialObject}s for the efficient execution of spatial joins.
 */

public abstract class SpatialIndex<RECORD extends Record>
{
    /**
     * Returns the {@link com.geophile.z.Space} associated with this SpatialIndex.
     * @return The {@link com.geophile.z.Space} associated with this SpatialIndex.
     */
    public final Space space()
    {
        return space;
    }

    /**
     * Adds the given record to the index, keyed by the given {@link com.geophile.z.SpatialObject}. A search of
     * this index given a {@link com.geophile.z.SpatialObject} overlapping the given key will locate the given record.
     * @param key The {@link com.geophile.z.SpatialObject} being indexed.
     * @param record The record to be added.
     */
    public abstract void add(SpatialObject key, RECORD record) throws IOException, InterruptedException;

    /**
     * Removes from this index the record associated with the given {@link com.geophile.z.SpatialObject}.
     * A number of records may be located during the removal. The given {@link com.geophile.z.RecordFilter}
     * will identify the records to be removed.
     * @param spatialObject Key of the records to be removed.
     * @param recordFilter Identifies the exact records to be removed, causing false positives to be ignored.
     * @return true if spatialObject was found and removed, false otherwise
     */
    public abstract boolean remove(SpatialObject spatialObject,
                                   RecordFilter<RECORD> recordFilter) throws IOException, InterruptedException;

    /**
     * Creates a SpatialIndex. The index
     * should never be manipulated directly at any time. It is intended to be maintained and searched only
     * through the interface of this class.
     * @param space The {@link Space} containing the {@link SpatialObject}s to be indexed.
     * @param index The {@link Index} that will store the indexed {@link SpatialObject}s.
     */
    public static <RECORD extends Record> SpatialIndex<RECORD> newSpatialIndex(Space space,
                                                                               Index<RECORD> index)
        throws IOException, InterruptedException
    {
        return newSpatialIndex(space, index, Options.DEFAULT);
    }

    /**
     * Creates a SpatialIndex. The index
     * should never be manipulated directly at any time. It is intended to be maintained and searched only
     * through the interface of this class.
     * @param space The {@link Space} containing the {@link SpatialObject}s to be indexed.
     * @param index The {@link Index} that will store the indexed {@link SpatialObject}s.
     */
    public static <RECORD extends Record> SpatialIndex<RECORD> newSpatialIndex(Space space,
                                                                               Index<RECORD> index,
                                                                               Options options)
        throws IOException, InterruptedException
    {
        return new SpatialIndexImpl<>((SpaceImpl) space, index, options);
    }

    // For use by subclasses

    protected SpatialIndex(SpaceImpl space, Index<RECORD> index, Options options)
    {
        this.space = space;
        this.index = index;
        this.options = options;
    }

    // Object state

    protected final SpaceImpl space;
    protected final Index<RECORD> index;
    protected final Options options;

    // Inner classes

    public enum Options {DEFAULT, SINGLE_CELL}

    public static class Exception extends RuntimeException
    {
        public Exception(String message)
        {
            super(message);
        }
    }
}
