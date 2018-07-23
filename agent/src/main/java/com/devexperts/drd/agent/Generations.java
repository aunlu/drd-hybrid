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

import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.agent.util.QuickSort;

import java.util.Arrays;

public class Generations {
    private static final long[] EMPTY_ARRAY = new long[0];
    public static volatile int generation = 0;
    public static volatile long[] dead = EMPTY_ARRAY;
    public static volatile long[] sortedDead = EMPTY_ARRAY;
    public static final Counter diffProfiler = new Counter();

    public static synchronized void threadDied(long tid) {
        dead = Arrays.copyOf(dead, dead.length + 1);
        dead[dead.length - 1] = tid;
        long[] newSortedDead = Arrays.copyOf(sortedDead, sortedDead.length + 1);
        int insertIndex = -Arrays.binarySearch(newSortedDead, tid) - 1;
        if (insertIndex < 0)
            throw new IllegalStateException("Tid " + tid + " is already dead : " + Arrays.toString(newSortedDead));
        ArrayUtils.insert(newSortedDead, insertIndex, tid, sortedDead.length);
        sortedDead = newSortedDead; //atomic link update
        generation++;
    }

    public static long[] getDiff(int fromGen) {
        int gen = generation;
        if (fromGen >= gen) return EMPTY_ARRAY;
        //In fact, sortedDead link may already contain info about later generations, but it doesn't matter.
        if (fromGen == 0) return sortedDead;
        //TODO cache sorted subarrays somehow
        //TODO WHY SORT each time?
        diffProfiler.increment();
        long[] res = new long[gen - fromGen];
        System.arraycopy(dead, fromGen, res, 0, gen - fromGen);
        QuickSort.sort(res);
        return res;
    }
}
