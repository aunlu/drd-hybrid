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
 * Fast manager of isolated per-thread flags. Initially each flag is released.
 */
class FlagManager {
    //TODO make it field of thread class
    private final ThreadLocal<Boolean> threadLocal = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    /**
     * Raise flag for current thread if it was released
     *
     * @return true iff flag state has changed (i.e. was released)
     */
    public boolean raiseIfReleased() {
        boolean raised = get();
        if (raised) {
            return false;
        } else {
            raise();
            return true;
        }
    }

    /**
     * Raises flag for current thread
     */
    public void raise() {
        threadLocal.set(Boolean.TRUE);
    }

    /**
     * Releases flag for current thread
     */
    public void release() {
        threadLocal.set(Boolean.FALSE);
    }

    /**
     * @return flag state of current thread (true if raised, false if released)
     */
    public boolean get() {
        return threadLocal.get();
    }
}
