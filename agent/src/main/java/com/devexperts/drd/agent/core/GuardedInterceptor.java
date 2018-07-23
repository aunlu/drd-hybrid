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

import com.devexperts.drd.bootstrap.*;

/**
 * Decorator, that protects actual interceptor from entering endless loops in heavy internal calls, that might occur
 * if call processing requires usage of instrumented data structures.
 * <p/>
 * Implemented via lock-like try-finally blocks: raise flag -> call super -> release, if it was released initially.
 */
public class GuardedInterceptor implements DRDInterceptor {
    private final DRDInterceptor delegate;
    private final Guard guard = Guard.INSTANCE;

    public GuardedInterceptor(DRDInterceptor delegate) {
        this.delegate = delegate;
        boolean locked = guard.isLockedSoft();
        //INIT internals
        if (locked) {
            guard.unlockSoft();
            guard.lockSoft();
        } else {
            guard.lockSoft();
            guard.unlockSoft();
        }
        DRDLogger.log(getClass().getSimpleName() + " initialized " + (locked == guard.isLockedSoft()));
    }

    public void beforeWait(Object o, int callerId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.beforeWait(o, callerId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterWait(Object o, int callerId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterWait(o, callerId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void beforeVolatileWrite(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.beforeVolatileWrite(ref, ownerId, nameId, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void beforeManualSyncSend(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.beforeManualSyncSend(o, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void beforeMonitorExit(Object o, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.beforeMonitorExit(o, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterVolatileRead(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterVolatileRead(ref, ownerId, nameId, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterManualSyncReceive(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterManualSyncReceive(o, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterManualSyncFullHB(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterManualSyncFullHB(o, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterMonitorEnter(Object o, int callerId, int callerNameId, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterMonitorEnter(o, callerId, callerNameId, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterRead(IDataClock clock, int location, boolean track, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterRead(clock, location, track, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterWrite(IDataClock clock, int location, boolean track, boolean print) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterWrite(clock, location, track, print);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void afterForeignRead(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.afterForeignRead(o, callerId, location, track, print, detectWWOnly);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public void beforeForeignWrite(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {
        boolean raised = guard.lockSoftIfUnlocked();
        try {
            delegate.beforeForeignWrite(o, callerId, location, track, print, detectWWOnly);
        } finally {
            if (raised) {
                guard.unlockSoft();
            }
        }
    }

    public int status() {
        return guard.status();
    }

    public int lockSoft() {
        return guard.lockSoft();
    }

    public int unlockSoft() {
        return guard.unlockSoft();
    }

    public int lockHard() {
        return guard.lockHard();
    }

    public int unlockHard() {
        return guard.unlockHard();
    }

    public void beforeStart(Thread t) {
        delegate.beforeStart(t);
    }

    public void afterJoin(Thread t) {
        delegate.afterJoin(t);
    }

    public void beforeDying() {
        delegate.beforeDying();
    }
}
