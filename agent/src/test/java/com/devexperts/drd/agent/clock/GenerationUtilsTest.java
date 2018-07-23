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

package com.devexperts.drd.agent.clock;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class GenerationUtilsTest {
    @Test
    public void testSameArrays() {
        int[] a = new int[]{1, 2};
        int[] b = a;
        Assert.assertTrue(b == GenerationUtils.propagateGeneration(a, b));
    }

    @Test
    public void simpleTestExactlyIn() {
        int[] a = new int[]{1, 2, 3};
        int[] b = new int[]{1, 2};
        Assert.assertTrue(a == GenerationUtils.propagateGeneration(a, b));
        b = new int[0];
        Assert.assertTrue(a == GenerationUtils.propagateGeneration(a, b));
    }

    @Test
    public void testNotExactlyIn() {
        int[] a = new int[]{51, 62, 73};
        int[] b = new int[]{82, 94, 105};
        checkSameContent(GenerationUtils.propagateGeneration(a, b), new int[]{51 | 82, 94 | 62, 105 | 73});
        b = new int[]{32, 46};
        checkSameContent(GenerationUtils.propagateGeneration(a, b), new int[]{51 | 32, 62 | 46, 73});
    }

    @Test
    public void testUpdateMask() {
        Assert.assertArrayEquals(GenerationUtils.updateMask(new int[0], new int[0]), new int[0]);
        Assert.assertArrayEquals(GenerationUtils.updateMask(new int[]{1 << 3 | 1 << 7}, new int[0]), new int[]{1 << 3 | 1 << 7});
        Assert.assertArrayEquals(GenerationUtils.updateMask(new int[]{1 << 3 | 1 << 7}, new int[]{5}), new int[]{1 << 3 | 1 << 7 | 1 << 5});
        Assert.assertArrayEquals(GenerationUtils.updateMask(new int[]{1 << 3 | 1 << 7}, new int[]{95}), new int[]{1 << 3 | 1 << 7, 0, 1 << 31});
    }

    private void checkSameContent(int[] a, int[] b) {
        Arrays.sort(a);
        Arrays.sort(b);
        Assert.assertArrayEquals(a, b);
    }
}
