/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialobject.jts;

import com.geophile.z.Space;
import com.geophile.z.SpatialObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class JTSLineString extends JTSBaseWithBoundingBox
{
    // SpatialObject interface (not implemented by JTSBase)

    @Override
    public double[] arbitraryPoint()
    {
        double[] point = new double[2];
        Coordinate coordinate = lineString().getCoordinate();
        point[0] = coordinate.x;
        point[1] = coordinate.y;
        return point;
    }

    @Override
    public boolean equalTo(SpatialObject that)
    {
        boolean eq = false;
        if (that != null && that instanceof JTSLineString) {
            LineString thatLineString = ((JTSLineString) that).lineString();
            eq = lineString().equals(thatLineString);
        }
        return eq;
    }

    // JTSLineString interface

    public JTSLineString(Space space, LineString lineString)
    {
        super(space, lineString);
    }

    // For use by this class

    private LineString lineString()
    {
        return (LineString) geometry;
    }
}
