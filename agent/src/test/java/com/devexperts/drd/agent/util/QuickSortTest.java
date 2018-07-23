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

package com.devexperts.drd.agent.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class QuickSortTest {
    @Test
    public void test() {
        check();
        check(0);
        check(-1, 0, 1);
        check(1, 0, -1);
        check(6, 2, 5, -1, 0, -6);
    }

    private void check(long... values) {
        long[] a1 = Arrays.copyOf(values, values.length);
        long[] a2 = Arrays.copyOf(values, values.length);
        Arrays.sort(a1);
        QuickSort.sort(a2);
        Assert.assertArrayEquals(a1, a2);
    }
}
