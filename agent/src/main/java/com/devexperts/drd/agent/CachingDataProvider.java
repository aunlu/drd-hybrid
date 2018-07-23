/*
 * DRD - Dynamic Data Race Detector for Java programs
 *
 * Copyright (C) 2002-2018 Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.devexperts.drd.agent;

import com.devexperts.drd.agent.clock.DataClock;
import com.devexperts.drd.agent.core.ClocksStorage;
import com.devexperts.drd.agent.core.CompositeKeysCache;
import com.devexperts.drd.agent.core.HBDynamicHelper;
import com.devexperts.drd.bootstrap.*;

public class CachingDataProvider implements DataProvider {
    private final CompositeKeysCache compositeKeysCache;
    private final HBDynamicHelper hbDynamicHelper;

    public CachingDataProvider(ITransformation transformation) {
        this.compositeKeysCache = new CompositeKeysCache(transformation.getCompositeKeysManager());
        //init internals
        try {
            if (this.compositeKeysCache.getCachedManualSyncWD(1) != null) {
                BlackHole.BLACK_HOLE.print(getClass().getSimpleName() + " initialized.");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            DRDLogger.debug("IGNORE ME", e);
        }
        this.hbDynamicHelper = new HBDynamicHelper(transformation.getHBManager());
    }

    public AbstractWeakDisposable getWeakDisposableSample(int hbContractId) {
        return compositeKeysCache.getCachedManualSyncWD(hbContractId);
    }

    public IDataClock createNewDataClock(int ownerId) {
        return new DataClock(ownerId);
    }

    public boolean matchesToHBVertex(Class c, String methodName, int hbVertexId) {
        return hbDynamicHelper.matches(c, methodName, hbVertexId);
    }

    public ISyncClock getVolatileSyncClock(Object ref, int ownerId, int nameId) {
        VolatileWeakDisposableBillet wd = compositeKeysCache.getCachedVolatileWD(ref, ownerId, nameId);
        ISyncClock volatilesClock = ClocksStorage.getVolatilesClock(wd);
        if (volatilesClock == null) {
            wd.clear();
            return ClocksStorage.getOrCreateVolatilesClock(compositeKeysCache.createNewVolatileWD(ref, ownerId, nameId));
        }
        return volatilesClock;
    }
}
