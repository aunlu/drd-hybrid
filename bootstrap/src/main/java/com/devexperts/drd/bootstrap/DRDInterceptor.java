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

package com.devexperts.drd.bootstrap;

/**
 * Processes all significant events in target application. DRD facade, that hides all implementation details: clocks, race detection internals, etc.
 * Calls of interceptor's methods are injected into application byte code by DRD transformer.
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface DRDInterceptor {
    /**
     * To be invoked before executing {@link Object#notify()} or {@link Object#notifyAll()} method
     *
     * @param o      - object, whose instance "wait" method would be executed
     * @param callerId - id of class of object, that called this method
     */
    public void beforeWait(Object o, int callerId, boolean print);

    /**
     * To be invoked after returning from {@link Object#wait()} method
     *
     * @param o      - object, whose instance "wait" method returned
     * @param callerId - id of class of object, that called this method
     */
    public void afterWait(Object o, int callerId, boolean print);

    /**
     * To be invoked before executing {@link Thread#start()} method
     *
     * @param t - child thread
     */
    public void beforeStart(Thread t);

    /**
     * To be invoked after returning from {@link Thread#join()} method
     *
     * @param t - dead thread
     */
    public void afterJoin(Thread t);

    /**
     * To be invoked before return from current thread's run() or interrupt() method
     */
    public void beforeDying();

    /**
     * To be invoked before writing volatile variable
     */
    public void beforeVolatileWrite(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked before manual synchronization send
     *
     * @param o object, associated with that manual synchronization
     */
    public void beforeManualSyncSend(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked before releasing any monitor
     *
     * @param o object, whose monitor would be released
     */
    public void beforeMonitorExit(Object o, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked after reading volatile variable
     */
    public void afterVolatileRead(Object ref, int ownerId, int nameId, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked after manual synchronization receive
     *
     * @param o object, associated with that manual synchronization
     */
    public void afterManualSyncReceive(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked after manual synchronization full
     *
     * @param o object, associated with that manual synchronization
     */
    public void afterManualSyncFullHB(AbstractWeakDisposable o, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked after acquiring any monitor (by means of "synchronized" keyword)
     *
     * @param o object, whose monitor was just acquired
     */

    public void afterMonitorEnter(Object o, int callerId, int callerNameId, boolean print);

    /**
     * To be invoked before reading any instance field
     *
     * @param clock field's clock
     */
    public void afterRead(IDataClock clock, int location, boolean track, boolean print);

    /**
     * To be invoked before writing any instance field
     *
     * @param clock field's clock
     */
    public void afterWrite(IDataClock clock, int location, boolean track, boolean print);

    public void afterForeignRead(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly);

    public void beforeForeignWrite(Object o, int callerId, int location, boolean track, boolean print, boolean detectWWOnly);

    public int status();

    public int lockSoft();

    public int unlockSoft();

    public int lockHard();

    public int unlockHard();
}
