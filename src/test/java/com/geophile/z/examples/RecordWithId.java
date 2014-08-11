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

package com.geophile.z.examples;

import com.geophile.z.SpatialObject;
import com.geophile.z.index.BaseRecord;

public class RecordWithId extends BaseRecord
{
    @Override
    public boolean equals(Object obj)
    {
        boolean eq = false;
        if (obj != null && obj instanceof RecordWithId) {
            RecordWithId that = (RecordWithId) obj;
            eq = this.z() == that.z() && this.id == that.id;
        }
        return eq;
    }

    public int id()
    {
        return id;
    }

    public void id(int newId)
    {
        id = newId;
    }

    public RecordWithId(SpatialObject spatialObject)
    {
        spatialObject(spatialObject);
        id = 0;
    }

    public RecordWithId(SpatialObject spatialObject, int id)
    {
        spatialObject(spatialObject);
        this.id = id;
    }

    public RecordWithId()
    {}

    private int id;
}
