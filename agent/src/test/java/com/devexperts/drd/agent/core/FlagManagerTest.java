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

public class FlagManagerTest {
    private final FlagManager flagManager = new FlagManager();

    @Test
    public void singleThreadTest() {
        checkFlag(false);
        flagManager.raise();
        checkFlag(true);
        Assert.assertFalse(flagManager.raiseIfReleased());
        checkFlag(true);
        flagManager.release();
        checkFlag(false);
        Assert.assertTrue(flagManager.raiseIfReleased());
        checkFlag(true);
    }

    private void checkFlag(boolean raised) {
        Assert.assertSame(raised, flagManager.get());
    }
}
