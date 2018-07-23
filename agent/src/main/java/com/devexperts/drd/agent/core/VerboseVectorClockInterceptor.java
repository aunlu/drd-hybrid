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

import com.devexperts.drd.bootstrap.DataRaceDescription;
import com.devexperts.drd.agent.ThreadUtils;
import com.devexperts.drd.agent.clock.DataClock;
import com.devexperts.drd.agent.clock.ThreadVectorClock;
import com.devexperts.drd.agent.race.RaceReporter;
import com.devexperts.drd.bootstrap.*;

/**
 * Vector clock-based implementation of {@link com.devexperts.drd.bootstrap.DRDInterceptor}, that processes all significant application events.
 * Can't be used directly, use {@link GuardedInterceptor} instead.
 */
public class VerboseVectorClockInterceptor implements DRDInterceptor {
    private final DRDRegistry registry = DRDEntryPoint.getRegistry();
    private final RaceReporter raceReporter = new RaceReporter(registry);
    private final DataProvider dataProvider = DRDEntryPoint.getDataProvider();

    public void beforeWait(Object o, int callerId, boolean print) {
        //check(caller);
        ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock synClock = ClocksStorage.getSynClock(o);
        doBeforeMonitorExit(threadClock, synClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " is calling wait() in class " + registry.getClassName(callerId)
                    + " on object " + "with vc " + synClock + ". TC: " + threadClock);
        }
    }

    public void afterWait(Object o, int callerId, boolean print) {
        //check(caller);
        ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock synClock = ClocksStorage.getSynClock(o);
        doAfterMonitorEnter(threadClock, synClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " is returning from wait() in class " + registry.getClassName(callerId)
                    + " on object " + "with vc " + synClock + ". TC: " + threadClock);
        }
    }

    public void beforeStart(Thread t) {
        if (DRDProperties.soutEnabled) {
            DRDLogger.log("before start of thread " + t.getName() + " (tid=" + t.getId() + ") in thread " + Thread.currentThread().getId());
        }
        ClocksStorage.onFork(t.getId());
    }

    public void afterJoin(Thread t) {
        ClocksStorage.afterJoin(t.getId());
    }

    public void beforeDying() {
        ClocksStorage.onDie();
    }

    public void afterMonitorEnter(Object o, int callerId, int callerNameId, boolean print) {
        //check(caller);
        ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock synClock = ClocksStorage.getSynClock(o);
        doAfterMonitorEnter(threadClock, synClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " entered monitor in " + extractCall(callerId, callerNameId)
                    + "() with vc " + synClock + ". TC: " + threadClock);
        }
    }

    public void afterManualSyncReceive(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        //check(caller);
        ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock manualSynClock = getManualSyncClock(o);
        doAfterMonitorEnter(threadClock, manualSynClock);
        if (print) {
            final String s = "Thread " + threadClock.tid + " received manual sync in " + extractCall(callerId, callerNameId)
                    + "on " + o + " " + o + "() with vc " + manualSynClock + ". TC: " + threadClock;
            DRDLogger.log(s);
        }
    }

    public void afterManualSyncFullHB(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        //check(caller);
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        threadClock.tick();
        final ISyncClock manualSynClock = getManualSyncClock(o);
        manualSynClock.loadTwoWay(threadClock);
        if (print) {
            final String s = "Thread " + threadClock.tid + " full manual sync in " + extractCall(callerId, callerNameId)
                    + "on " + o + " " + o + "() with vc " + manualSynClock + ". TC: " + threadClock;
            DRDLogger.log(s);
        }
    }

    public void afterVolatileRead(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {
        //check(caller);
        //check(owner);
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        ISyncClock volatilesClock = dataProvider.getVolatileSyncClock(ref, ownerId, nameId);
        doAfterMonitorEnter(threadClock, volatilesClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " read volatile in " + extractCall(callerId, callerNameId)
                    + "() with vc " + volatilesClock + ". TC: " + threadClock);
        }
    }

    private ISyncClock getManualSyncClock(AbstractWeakDisposable o) {
        ISyncClock clock = ClocksStorage.getManualSynClock(o);
        if (clock == null) {
            clock = ClocksStorage.getOrCreateManualSynClock(o.copy());
        }
        return clock;
    }

    private void doAfterMonitorEnter(ThreadVectorClock threadClock, ISyncClock synClock) {
        synClock.loadTo(threadClock);
    }

    public void beforeMonitorExit(Object o, int callerId, int callerNameId, boolean print) {
        //check(caller);
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock synClock = ClocksStorage.getSynClock(o);
        doBeforeMonitorExit(threadClock, synClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " released monitor in " + extractCall(callerId, callerNameId)
                    + "() with vc " + synClock + ". TC: " + threadClock);
        }
    }

    public void beforeManualSyncSend(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {
        //check(caller);
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final ISyncClock manualSynClock = getManualSyncClock(o);
        doBeforeMonitorExit(threadClock, manualSynClock);
        if (print) {
            final String s = "Thread " + threadClock.tid + " sent manual sync in " + extractCall(callerId, callerNameId)
                    + "on " + o + " " + o + "() with vc " + manualSynClock + ". TC: " + threadClock;
            DRDLogger.log(s);
        }
    }

    public void beforeVolatileWrite(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {
        //check(caller);
        //check(owner);
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        ISyncClock volatilesClock = dataProvider.getVolatileSyncClock(ref, ownerId, nameId);
        doBeforeMonitorExit(threadClock, volatilesClock);
        if (print) {
            DRDLogger.log("Thread " + threadClock.tid + " writes volatile in " + extractCall(callerId, callerNameId)
                    + "() with vc " + volatilesClock + ". TC: " + threadClock);
        }
    }

    private void doBeforeMonitorExit(ThreadVectorClock threadClock, ISyncClock synClock) {
        threadClock.tick();
        synClock.loadFrom(threadClock);
    }

    //TODO copy/pastes: read-write, afterForeignRead-beforeForeignWrite
    public void afterRead(IDataClock clock, int location, boolean track, boolean print) {
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        StringBuilder msg = null;
        if (print) {
            Location l = registry.getLocation(location);
            msg = new StringBuilder("Thread ").append(threadClock.tid).append(" READ FIELD ")
                    .append(extractCall(l))
                    .append(". VC : ").append(clock).append(", TC: ").append(threadClock);
        }
        DataRaceDescription race = clock.read(threadClock, location, false);
        if (race != null) {
            reportFieldRace(race);
        }
        if (print) {
            msg.append(" ---> VC: ").append(clock);
            DRDLogger.log(msg.toString());
        }
        if (track) {
            Location l = registry.getLocation(location);
            registry.getRaceHistory().trackFieldAccesses(l.targetOwnerId, l.targetNameId, location);
        }
    }

    private void reportFieldRace(DataRaceDescription race) {
        Location l = registry.getLocation(race.racingLocationId);
        raceReporter.reportRace(DataRaceDescription.RaceTarget.FIELD, race,
                registry.getRaceHistory().getFieldAccess(l.targetOwnerId, l.targetNameId, race.racingTid));
    }

    public void afterWrite(IDataClock clock, int location, boolean track, boolean print) {
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        StringBuilder msg = null;
        if (print) {
            Location l = registry.getLocation(location);
            msg = new StringBuilder("Thread ").append(threadClock.tid).append(" WRITE FIELD ")
                    .append(extractCall(l))
                    .append(". VC : ").append(clock).append(", TC: ").append(threadClock);
        }
        DataRaceDescription race = clock.write(threadClock, location, false);
        if (race != null) {
            reportFieldRace(race);
        }
        if (print) {
            msg.append(" ---> VC: ").append(clock);
            DRDLogger.log(msg.toString());
        }
        if (track) {
            Location l = registry.getLocation(location);
            registry.getRaceHistory().trackFieldAccesses(l.targetOwnerId, l.targetNameId, location);
        }
    }

    public void afterForeignRead(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final DataClock fvc = ClocksStorage.getForeignClock(callerId, o);
        StringBuilder msg = null;
        if (print) {
            Location l = registry.getLocation(location);
            msg = new StringBuilder(ThreadUtils.getThreadDescription(threadClock.tid)).append(" READ METHOD ")
                    .append(extractCall(l)).append(". VC : ").append(fvc).append(", TC: ").append(threadClock).append("\n");
        }
        DataRaceDescription race = fvc.read(threadClock, location, detectWWOnly);
        if (race != null) {
            reportForeignRace(o, race);
        }
        if (track) {
            Location l = registry.getLocation(location);
            registry.getRaceHistory().trackForeignCall(o, l.targetOwnerId, location);
        }
        if (print) {
            msg.append(" ---> VC: ").append(fvc);
            DRDLogger.log(msg.toString());
        }
    }

    private void reportForeignRace(Object o, DataRaceDescription race) {
        if (DRDProperties.reportForeignRaces) {
            raceReporter.reportRace(DataRaceDescription.RaceTarget.OBJECT, race,
                    registry.getRaceHistory().getObjectAccess(o, registry.getLocation(race.racingLocationId).targetOwnerId, race.racingTid));
        }
    }

    public void beforeForeignWrite(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {
        final ThreadVectorClock threadClock = getCurrentThreadClock();
        if (threadClock == null) return;
        final DataClock fvc = ClocksStorage.getForeignClock(callerId, o);
        StringBuilder msg = null;
        if (print) {
            Location l = registry.getLocation(location);
            msg = new StringBuilder(ThreadUtils.getThreadDescription(threadClock.tid)).append(" WRITE METHOD ")
                    .append(extractCall(l)).append(". VC : ").append(fvc).append(", TC: ").append(threadClock).append("\n");
        }
        DataRaceDescription race = fvc.write(threadClock, location, detectWWOnly);
        if (race != null) {
            reportForeignRace(o, race);
        }
        if (track) {
            Location l = registry.getLocation(location);
            registry.getRaceHistory().trackForeignCall(o, l.targetOwnerId, location);
        }
        if (print) {
            msg.append(" ---> VC: ").append(fvc);
            DRDLogger.log(msg.toString());
        }
    }

    private ThreadVectorClock getCurrentThreadClock() {
        return ClocksStorage.getThreadClock();
    }

    public int status() {
        throw new UnsupportedOperationException();
    }

    public int lockSoft() {
        throw new UnsupportedOperationException();
    }

    public int unlockSoft() {
        throw new UnsupportedOperationException();
    }

    public int lockHard() {
        throw new UnsupportedOperationException();
    }

    public int unlockHard() {
        throw new UnsupportedOperationException();
    }

    private String extractCall(Location l) {
        return extractCall(l.targetOwnerId, l.targetNameId) + " @ " + extractCall(l.callerOwnerId, l.callerNameId);
    }

    private String extractCall(int ownerId, int nameId) {
        return registry.getClassName(ownerId) + "." + registry.getFieldOrMethodName(nameId);
    }
}
