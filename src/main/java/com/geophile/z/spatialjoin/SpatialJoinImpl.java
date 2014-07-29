/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.*;
import com.geophile.z.index.sortedarray.SortedArray;
import com.geophile.z.space.SpatialIndexImpl;

import java.io.IOException;
import java.util.Iterator;

public class SpatialJoinImpl extends SpatialJoin
{
    public SpatialJoinImpl(SpatialJoinFilter filter, Duplicates duplicates)
    {
        super(filter, duplicates);
    }

    public Iterator<Pair> iterator(SpatialIndex leftSpatialIndex, SpatialIndex rightSpatialIndex)
        throws IOException, InterruptedException
    {
        if (!leftSpatialIndex.space().equals(rightSpatialIndex.space())) {
            throw new SpatialJoinException("Attempt to join spatial indexes with incompatible spaces");
        }
        Iterator<Pair> iterator =
            SpatialJoinIterator.pairIterator((SpatialIndexImpl) leftSpatialIndex,
                                             (SpatialIndexImpl) rightSpatialIndex,
                                             filter);
        if (duplicates == Duplicates.EXCLUDE) {
            iterator = new DuplicateEliminatingIterator<>(iterator);
        }
        return iterator;
    }

    public Iterator<SpatialObject> iterator(SpatialObject query, SpatialIndex dataIndex)
        throws IOException, InterruptedException
    {
        SpatialIndex queryIndex = SpatialIndex.newSpatialIndex(dataIndex.space(),
                                                               new SortedArray(),
                                                               query.maxZ() == 1
                                                               ? SpatialIndex.Options.SINGLE_CELL
                                                               : SpatialIndex.Options.DEFAULT);
        queryIndex.add(query);
        Iterator<SpatialObject> iterator =
            SpatialJoinIterator.spatialObjectIterator((SpatialIndexImpl) queryIndex,
                                                      (SpatialIndexImpl) dataIndex,
                                                      filter);
        if (duplicates == Duplicates.EXCLUDE) {
            iterator = new DuplicateEliminatingIterator<>(iterator);
        }
        return iterator;
    }

    public static boolean singleCellOptimization()
    {
        return Boolean.valueOf(System.getProperty(SINGLE_CELL_OPTIMIZATION_PROPERTY, "true"));
    }

    public static final String SINGLE_CELL_OPTIMIZATION_PROPERTY = "singlecellopt";
}
