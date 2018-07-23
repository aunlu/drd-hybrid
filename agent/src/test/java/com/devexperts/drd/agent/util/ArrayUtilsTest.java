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

import com.devexperts.drd.agent.ArrayUtils;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Random;

public class ArrayUtilsTest {
    private final Random random = new Random();

    private long[] createRandomArray(int length, int minValue, int maxValue) {
        final long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = minValue < 0 ? random.nextInt(minValue + maxValue + 1) + minValue : minValue == 0 ?
                    random.nextInt(maxValue + 1) : random.nextInt(maxValue - minValue + 1) + minValue;
        }
        return array;
    }

    @Test
    public void testCreateRandomArray() {
        for (int i = 10; i < 100; i++) {
            int min = i % 2 == 0 ? i * i : -i * i;
            int max = i % 2 == 0 ? i * i : i * i * 2;
            final long[] array = createRandomArray(i, min, max);
            assertEquals(array.length, i);
            for (long value : array) {
                assertTrue(value >= min && value <= max);
            }
        }
    }

    @Test
    public void testIndexOf() {
        for (int i = 10; i < 20; i++) {
            int min = i % 2 == 0 ? i * i : -i * i;
            int max = i * i * i;
            final long[] array = createRandomArray(i, min, max);
            for (int j = min; j < max; j++) {
                final int indexOf = ArrayUtils.indexOf(array, j);
                if (indexOf >= 0) {
                    assertTrue(array[indexOf] == j);
                } else {
                    for (long k : array) {
                        assertTrue(k != j);
                    }
                }
            }
        }
    }

    @Test
    public void testDelete() {
        for (int i = 10; i < 100; i++) {
            int min = i % 2 == 0 ? i * i : -i * i;
            int max = i * i * i;
            final long[] array = createRandomArray(i, min, max);
            final long[] copy = new long[i];
            System.arraycopy(array, 0, copy, 0, i);
            final int index = random.nextInt(i);
            ArrayUtils.delete(array, index);
            assertEquals(array.length, i);
            for (int j = 0; j < i - 1; j++) {
                assertEquals(array[j], j < index ? copy[j] : copy[j + 1]);
            }
            assertEquals(array[i - 1], 0);
        }
    }

    @Test
    public void testInsert() {
        long[] a = new long[]{0};
        ArrayUtils.insert(a, 0, 1, 0);
        Assert.assertArrayEquals(a, new long[]{1});
        a = new long[]{1, 2, 3, 4, 6, 7, 8, 9, 0};
        ArrayUtils.insert(a, 4, 5, 8);
        Assert.assertArrayEquals(a, new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
    }
}
