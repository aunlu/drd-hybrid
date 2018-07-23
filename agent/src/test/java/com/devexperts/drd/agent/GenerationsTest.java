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

import org.junit.Assert;
import org.junit.Test;

public class GenerationsTest {
    @Test
    public void testGenerations() {
        //test threadDied & getDiff
        Assert.assertEquals(Generations.generation, 0);
        Generations.threadDied(1/*, 5*/);
        Generations.threadDied(8/*, 6*/);
        Generations.threadDied(3/*, 7*/);
        Assert.assertEquals(Generations.generation, 3);
        //Assert.assertArrayEquals(Generations.deadTidsMask, new int[]{1 << 1 | 1 << 8 | 1 << 3});
        Assert.assertArrayEquals(Generations.getDiff(0), new long[]{1, 3, 8});
        Assert.assertArrayEquals(Generations.getDiff(1), new long[]{3, 8});
        Assert.assertArrayEquals(Generations.getDiff(2), new long[]{3});
        Generations.threadDied(9/*, 10*/);
        Assert.assertArrayEquals(Generations.getDiff(1), new long[]{3, 8, 9});
        Assert.assertArrayEquals(Generations.getDiff(2), new long[]{3, 9});
        Assert.assertArrayEquals(Generations.getDiff(3), new long[]{9});

/*        //test getFrame
        Assert.assertEquals(Generations.getDeadFrame(1, 0), 0);
        Assert.assertEquals(Generations.getDeadFrame(15, 0), 0);
        Assert.assertEquals(Generations.getDeadFrame(1, 1), 5);
        Assert.assertEquals(Generations.getDeadFrame(8, 1), 0);
        Assert.assertEquals(Generations.getDeadFrame(8, 2), 6);
        Assert.assertEquals(Generations.getDeadFrame(3, 3), 7);
        Assert.assertEquals(Generations.getDeadFrame(8, 3), 6);
        Assert.assertEquals(Generations.getDeadFrame(8, 4), 10);
        Assert.assertEquals(Generations.getDeadFrame(9, 4), 0);

        //test isDead
        Assert.assertFalse(Generations.isDead(new int[0], 1));
        Assert.assertTrue(Generations.isDead(new int[] {1 << 3 | 1 << 8}, 8));
        Assert.assertTrue(Generations.isDead(new int[] {1 << 3 | 1 << 8}, 3));
        Assert.assertFalse(Generations.isDead(new int[]{1 << 3 | 1 << 8}, 1));
        Assert.assertFalse(Generations.isDead(new int[]{1 << 2}, 8));*/
    }
}
