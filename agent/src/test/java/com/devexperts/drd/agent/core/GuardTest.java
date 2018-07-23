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

import org.junit.Assert;
import org.junit.Test;

public class GuardTest {
    private final Guard guard = Guard.INSTANCE;

    @Test
    public void testLock() {
        checkAvailable();
        guard.lockSoft();
        checkLockedSoft();
        guard.lockSoft();
        checkLockedSoft();
        Assert.assertFalse(guard.lockSoftIfUnlocked());
        checkLockedSoft();
        guard.unlockSoft();
        checkAvailable();
        Assert.assertTrue(guard.lockSoftIfUnlocked());
        Assert.assertFalse(guard.lockSoftIfUnlocked());
        checkLockedSoft();
        guard.unlockSoft();
        checkAvailable();
    }

    @Test
    public void testSemaphore() {
        checkAvailable();
        guard.lockHard(); //1
        checkLockedHard();
        guard.lockHard(); //2
        guard.lockHard(); //3
        checkLockedHard();
        guard.unlockHard(); //2
        checkLockedHard();
        guard.unlockHard(); //1
        checkLockedHard();
        guard.unlockHard(); //0
        checkAvailable();
    }

    @Test
    public void testCorrectness() {
        checkAvailable();
        guard.unlockHard();
        checkAvailable();
    }

    @Test
    public void testBoth() {
        checkAvailable();
        guard.lockSoft();
        checkLockedSoft();
        guard.lockHard(); //1
        guard.lockHard(); //2
        checkLockedHard();
        guard.unlockHard(); //1
        checkLockedHard();
        guard.unlockHard(); //0
        checkLockedSoft();
        guard.lockHard();
        guard.unlockSoft();
        checkLockedHard();
        guard.unlockHard();
        checkAvailable();
    }

    private void checkAvailable() {
        Assert.assertTrue(guard.status() == 0);
    }

    private void checkLockedSoft() {
        Assert.assertTrue(guard.status() == 1);
    }

    private void checkLockedHard() {
        Assert.assertTrue(guard.status() > 1);
    }
}
