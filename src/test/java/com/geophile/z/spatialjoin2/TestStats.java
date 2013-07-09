/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin2;

class TestStats
{
    void resetAll()
    {
        loadTimeMsec = 0;
        joinTimeNsec = 0;
        outputRowCount = 0;
        filterCount = 0;
        overlapCount = 0;
    }

    long loadTimeMsec;
    long joinTimeNsec;
    long outputRowCount;
    long filterCount;
    long overlapCount;
}
