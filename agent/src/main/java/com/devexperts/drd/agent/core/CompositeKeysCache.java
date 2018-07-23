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

package com.devexperts.drd.agent.core;

import com.devexperts.drd.agent.*;
import com.devexperts.drd.bootstrap.ICompositeKeysManager;
import com.devexperts.drd.bootstrap.AbstractVolatileWeakDisposable;
import com.devexperts.drd.bootstrap.AbstractWeakDisposable;
import com.devexperts.drd.bootstrap.VolatileWeakDisposableBillet;

public class CompositeKeysCache {
    private final ThreadLocal<VolatileWeakDisposableBillet> volatiles = new ThreadLocal<VolatileWeakDisposableBillet>() {
        @Override
        protected VolatileWeakDisposableBillet initialValue() {
            return new VolatileWeakDisposableBillet();
        }
    };

    private final ThreadLocal<AbstractWeakDisposable[]> manualSyncs = new ThreadLocal<AbstractWeakDisposable[]>() {
        @Override
        protected AbstractWeakDisposable[] initialValue() {
            return new AbstractWeakDisposable[compositeKeysManager.size()];
        }
    };

    private final ICompositeKeysManager compositeKeysManager;

    public CompositeKeysCache(ICompositeKeysManager compositeKeysManager) {
        this.compositeKeysManager = compositeKeysManager;
    }

    public VolatileWeakDisposableBillet getCachedVolatileWD(Object referent, int ownerId, int nameId) {
        VolatileWeakDisposableBillet billet = volatiles.get();
        billet.set(referent, ownerId, nameId);
        return billet;
    }

    public AbstractVolatileWeakDisposable createNewVolatileWD(Object referent, int ownerId, int nameId) {
        return new VolatileWeakDisposable(referent, ownerId, nameId);
    }

    public AbstractWeakDisposable getCachedManualSyncWD(int hbContractId) {
        AbstractWeakDisposable[] local = manualSyncs.get();
        AbstractWeakDisposable awd = local[hbContractId];
        if (awd == null) {
            awd = compositeKeysManager.getNewCompositeKeySample(hbContractId);
            local[hbContractId] = awd;
        }
        awd.hashcode = 1;
        return awd;
    }
}
