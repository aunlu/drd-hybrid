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

import com.devexperts.drd.bootstrap.AbstractWeakDisposable;
import com.devexperts.drd.bootstrap.DRDInterceptor;
import com.devexperts.drd.bootstrap.IDataClock;

public class MockInterceptor implements DRDInterceptor {
    public void beforeWait(Object o, int callerId, boolean print) {

    }

    public void afterWait(Object o, int callerId, boolean print) {

    }

    public void beforeStart(Thread t) {

    }

    public void afterJoin(Thread t) {

    }

    public void beforeDying() {

    }

    public void beforeVolatileWrite(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {

    }

    public void beforeManualSyncSend(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {

    }

    public void beforeMonitorExit(Object o, int callerId, int callerNameId, boolean print) {

    }

    public void afterVolatileRead(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print) {

    }

    public void afterManualSyncReceive(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {

    }

    public void afterManualSyncFullHB(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print) {

    }

    public void afterMonitorEnter(Object o, int callerId, int callerNameId, boolean print) {

    }

    public void afterRead(IDataClock clock, int location, boolean track, boolean print) {

    }

    public void afterWrite(IDataClock clock, int location, boolean track, boolean print) {

    }

    public void afterForeignRead(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {

    }

    public void beforeForeignWrite(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly) {

    }

    public int status() {
        return 0;
    }

    public int lockSoft() {
        return 0;
    }

    public int unlockSoft() {
        return 0;
    }

    public int lockHard() {
        return 0;
    }

    public int unlockHard() {
        return 0;
    }
}
