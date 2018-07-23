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

public class ArrayUtils {
    private ArrayUtils() {
    }

    public static int indexOf(long[] array, long value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) return i;
        }
        return -1;
    }

    public static void delete(long[] array, int index) {
        final int length = array.length;
        assert index >= 0 && index < length;
        int i = index;
        while (i < length - 1) {
            array[i] = array[++i];
        }
        array[length - 1] = 0;
    }

    public static void insert(long[] array, int index, long value, int size) {
        if (array.length - size < 1) throw new IllegalArgumentException();
        long tmp = value;
        for (int i = index; i < size; i++) {
            tmp = array[i];
            array[i] = value;
            value = tmp;
        }
        array[size] = tmp;
    }

    public static String toString(Object[] array) {
        if (array == null) return "";
        final StringBuilder sb = new StringBuilder();
        for (Object e : array) {
            sb.append(e).append("\n");
        }
        return sb./*append("...\n").*/toString();
    }
}
