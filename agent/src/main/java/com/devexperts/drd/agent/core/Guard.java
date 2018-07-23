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

/**
 * Per-thread guard containing of lock and counting semaphore.
 */
public class Guard {
    public static final Guard INSTANCE = new Guard();
    public static final int AVAILABLE = 0;
    public static final int LOCKED_SOFT = 1;
    private final ThreadLocal<Lock> locks = new ThreadLocal<Lock>() {
        @Override
        protected Lock initialValue() {
            return new Lock();
        }
    };

    private Guard() {}

    /**
     * <b>
     * Obtains guard status
     * <ul>
     * <li>0 = available</li>
     * <li>1 = locked soft</li>
     * <li>2+ = locked hard</li>
     * </ul>
     * </b>
     */
    public int status() {
        return locks.get().state;
    }

    /**
     * locks-soft guard by current thread and returns updated status
     */
    public int lockSoft() {
        Lock lock = locks.get();
        lock.state |= 1;
        return lock.state;
    }

    /**
     * unlocks-soft guard by current thread and returns updated status
     */
    public int unlockSoft() {
        Lock lock = locks.get();
        lock.state &= ~1;
        return lock.state;
    }

    /**
     * checks if guard is locked-soft
     */
    public boolean isLockedSoft() {
        return (locks.get().state & 1) == 1;
    }

    /**
     * locks-soft guard by current thread
     *
     * @return if guard soft-locked state was changed (i.e. if initially guard was unlocked-soft)
     */
    public boolean lockSoftIfUnlocked() {
        Lock lock = locks.get();
        if ((lock.state & 1) == 1) {
            return false;
        } else {
            locks.get().state |= 1;
            return true;
        }
    }

    /**
     * locks-hard this guard by current thread and returns updated status
     */
    public int lockHard() {
        Lock lock = locks.get();
        lock.state += 2;
        return lock.state;
    }

    /**
     * unlocks-hard this guard by current thread and returns updated status
     */
    public int unlockHard() {
        Lock lock = locks.get();
        if (lock.state >= 2) {
            lock.state -= 2;
        }
        return lock.state;
    }

    /**
     * @return string representation of guard for current thread
     */
    @Override
    public String toString() {
        Lock lock = locks.get();
        return ((lock.state & 1) == 1 ? "Soft-locked" : "Not soft-locked") + ", hard-locked " + (lock.state >> 1) +
                " times @ " + Thread.currentThread();
    }

    private static class Lock {
        //last bit is for soft-locking, other bits are for counting hard-locking
        int state;
    }
}
