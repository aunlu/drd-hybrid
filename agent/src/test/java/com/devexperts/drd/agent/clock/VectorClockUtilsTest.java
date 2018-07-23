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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class VectorClockUtilsTest {
    private final Random random = new Random();

    @Test
    public void testCreateRandomClock() {
        for (int i = 1; i < 1000; i++) {
            int maxTid = random.nextInt(i * 2) + 1;
            int maxFrame = i * i;
            final VectorClock vc = createRandomClock(i, maxTid, maxFrame);
            if (vc == null) {
                assertTrue(maxTid < i);
            } else {
                assertTrue(vc.size == i * 2);
                long prev = -1;
                for (int j = 0; j < vc.size; j += 2) {
                    assertTrue(vc.clock[j] <= maxTid);
                    assertTrue(vc.clock[j + 1] <= maxFrame);
                    assertTrue(prev < vc.clock[j]);
                    prev = vc.clock[j];
                }
            }
        }
    }

    @Test
    public void testBinarySearch() {
        assertEquals(-2, VectorClockUtils.findTid(new VectorClock(), 3));
        assertEquals(0, VectorClockUtils.findTid(create(new long[]{1, 2, 0, 0, 0, 0, 0, 0}, 2), 1));

        final Random rnd = new Random();
        int size = 2 * (rnd.nextInt(10) + 10);
        final long[] clock = new long[size + 6];
        int last = 0;
        for (int i = 0; i < size; i += 2) {
            clock[i] = last += (rnd.nextInt(5) + 2);
            clock[i + 1] = rnd.nextInt(100);
        }
        VectorClock vc = new VectorClock();
        vc.clock = clock;
        vc.size = size;
        for (int i = 0; i < size; i += 2) {
            int index = VectorClockUtils.findTid(vc, clock[i]);
            assertEquals(i, index);
        }
        assertEquals(-2, VectorClockUtils.findTid(vc, clock[0] - 1));
        for (int i = 0; i < size - 2; i += 2) {
            assertEquals(-i - 4, VectorClockUtils.findTid(vc, (clock[i] + clock[i + 2]) / 2));
        }
        assertEquals(-size - 2, VectorClockUtils.findTid(vc, clock[size - 2] + 1));
    }

    @Test
    public void testMergeSortedClocks() {
        performTestMergeSortedClocks(new long[0], 0, new long[0], 0, 0, new long[0]);
        performTestMergeSortedClocks(new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 10, new long[0], 0, 10,
                new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0});
        performTestMergeSortedClocks(new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 10, new long[]{2, 5, 5, 8, 6, 12, 7, 3, 8, 4, 9, 11, 10, 12}, 14, 18,
                new long[]{1, 2, 2, 5, 3, 4, 5, 8, 6, 12, 7, 8, 8, 4, 9, 11, 10, 12, 0, 0, 0, 0});
    }

    private void performTestMergeSortedClocks(long[] cl1, int length1, long[] cl2, int length2, int newSize, long[] expectedResult) {
        long[] result = new long[expectedResult.length];
        assertEquals(VectorClockUtils.mergeSortedClocks(cl1, length1, cl2, length2, result), newSize);
        assertArrayEquals(result, expectedResult);
    }

    @Test
    public void testAddFrameAndShift() {
        long tid = 10;
        long frame = 11;
        long[] clock = new long[]{0, 0};
        VectorClockUtils.addFrameAndShift(clock, tid, frame, 0, 0);
        assertArrayEquals(clock, new long[]{10, 11});
        clock = new long[]{1, 2, 23, 24, 0, 0};
        VectorClockUtils.addFrameAndShift(clock, tid, frame, 2, 4);
        assertArrayEquals(clock, new long[]{1, 2, 10, 11, 23, 24});
    }

    @Test
    public void testCheckIn() {
        long[] c1 = new long[0];
        long[] c2 = new long[0];
        assertTrue(VectorClockUtils.checkIn(c1, c2));
        c2 = new long[]{1, 2};
        assertTrue(VectorClockUtils.checkIn(c1, c2));
        c1 = new long[]{3, 4};
        assertFalse(VectorClockUtils.checkIn(c1, c2));
        c2 = new long[]{1, 2, 3, 2};
        assertFalse(VectorClockUtils.checkIn(c1, c2));
        c2 = new long[]{1, 2, 3, 4};
        assertTrue(VectorClockUtils.checkIn(c1, c2));
        c2 = new long[]{1, 6, 3, 8};
        assertTrue(VectorClockUtils.checkIn(c1, c2));
        c1 = new long[]{1, 7, 3, 4};
        assertFalse(VectorClockUtils.checkIn(c1, c2));
    }

    @Test
    public void testMerge() {
        long[] cl1 = new long[0];
        long[] cl2 = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        VectorClock vc1 = create(cl1, 0);
        VectorClock vc2 = create(cl2, 10);
        VectorClockUtils.load(vc1, vc2);
        assertTrue(sameClock(cl2, vc2));
        VectorClockUtils.load(vc2, vc1);
        assertTrue(sameClock(cl2, vc1));
        assertTrue(sameClock(cl2, vc2));
        cl1 = new long[]{-4, 16, 1, 5, 3, 2, 6, 14, 9, 1, 12, 22};
        vc1.clock = cl1;
        vc1.size = cl1.length;
        VectorClockUtils.load(vc1, vc2);
        assertTrue(VectorClockUtils.same(create(new long[]{-4, 16, 1, 5, 3, 4, 5, 6, 6, 14, 7, 8, 9, 10, 12, 22}), vc2));
    }

    @Test
    public void testDataRaceCheck() {
        final int currentTid = (int) Thread.currentThread().getId();
        long[] tclock = new long[(currentTid + 5) * 2];
        for (int i = 0; i < currentTid + 5; i++) {
            tclock[i * 2] = i;
            tclock[i * 2 + 1] = i * i;
        }
        VectorClock tvc = create(tclock);

        long[] vclock = new long[(currentTid + 5) * 2];
        for (int i = 0; i < currentTid + 5; i++) {
            vclock[i * 2] = tclock[i * 2];
            vclock[i * 2 + 1] = tclock[i * 2 + 1] - 1;
        }
        VectorClock fvc = create(vclock);
        assertEquals(-1, VectorClockUtils.checkDataRace(tvc, fvc));
        vclock[currentTid * 2 + 1]++;
        assertEquals(-1, VectorClockUtils.checkDataRace(tvc, fvc));
        vclock[(currentTid + 3) * 2 + 1]++;
        assertEquals(currentTid + 3, VectorClockUtils.checkDataRace(tvc, fvc));
        vclock[(currentTid + 3) * 2 + 1]--;
        tclock[(currentTid + 4) * 2] = 0;
        tclock[(currentTid + 4) * 2 + 1] = 0;
        tvc.size -= 2;
        assertEquals(currentTid + 4, VectorClockUtils.checkDataRace(tvc, fvc));
    }

    @Test
    public void testRemoveFrames() {
        long[] clock = new long[]{1, 10, 2, 20, 3, 30, 4, 40, 5, 50};
        VectorClock vc = new VectorClock();
        vc.clock = clock;
        vc.size = 10;
        long[] remove = new long[0];
        VectorClockUtils.removeFrames(vc, remove);
        assertArrayEquals(clock, new long[]{1, 10, 2, 20, 3, 30, 4, 40, 5, 50});
        assertSame(vc.size, 10);
        remove = new long[]{7, 12};
        VectorClockUtils.removeFrames(vc, remove);
        assertArrayEquals(clock, new long[]{1, 10, 2, 20, 3, 30, 4, 40, 5, 50});
        assertSame(vc.size, 10);
        remove = new long[]{3, 35, 5, 12, 7, 20};
        VectorClockUtils.removeFrames(vc, remove);
        assertArrayEquals(clock, new long[]{1, 10, 2, 20, 4, 40, 0, 0, 0, 0});
        assertSame(vc.size, 6);
        remove = new long[]{1, 35, 2, 12, 3, 20};
        VectorClockUtils.removeFrames(vc, remove);
        assertArrayEquals(clock, new long[]{4, 40, 0, 0, 0, 0, 0, 0, 0, 0});
        assertSame(vc.size, 2);
        remove = new long[]{1, 35, 2, 12, 3, 20, 4, 50};
        VectorClockUtils.removeFrames(vc, remove);
        assertArrayEquals(clock, new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        assertSame(vc.size, 0);
    }

    @Test
    public void testRemoveZeros() {
        long[] array = new long[0];
        assertEquals(VectorClockUtils.removeZeros(array), 0);
        assertArrayEquals(array, new long[0]);
        array = new long[]{0, 2};
        assertEquals(VectorClockUtils.removeZeros(array), 1);
        assertArrayEquals(array, new long[]{2, 0});
        array = new long[]{1, 2};
        assertEquals(VectorClockUtils.removeZeros(array), 2);
        assertArrayEquals(array, new long[]{1, 2});
        array = new long[]{1, 2, 0, 0, 0};
        assertEquals(VectorClockUtils.removeZeros(array), 2);
        assertArrayEquals(array, new long[]{1, 2, 0, 0, 0});
        array = new long[]{1, 0, 2, 0, 0, 3, 4, 0, 0, 5};
        assertEquals(VectorClockUtils.removeZeros(array), 5);
        assertArrayEquals(array, new long[]{1, 2, 3, 4, 5, 0, 0, 0, 0, 0});
        array = new long[]{0, 0, 1, 2, 0, 3, 0, 4, 5, 6, 0, 0, 0, 7, 8, 0, 9};
        assertEquals(VectorClockUtils.removeZeros(array), 9);
        assertArrayEquals(array, new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 0, 0});
    }

    @Test
    public void testRemoveFrameAndShift() {
        performRemoveFrameAndShiftTest(new long[]{1, 1}, 2, 0, new long[]{0, 0});
        performRemoveFrameAndShiftTest(new long[]{1, 2, 3, 4, 5, 6, 0, 0}, 6, 4, new long[]{1, 2, 3, 4, 0, 0, 0, 0});
        performRemoveFrameAndShiftTest(new long[]{1, 2, 3, 4, 5, 6, 0, 0}, 6, 2, new long[]{1, 2, 5, 6, 0, 0, 0, 0});
    }

    private void performRemoveFrameAndShiftTest(long[] arr, int size, int index, long[] expectedResult) {
        VectorClockUtils.removeFrameAndShift(arr, index, size);
        assertArrayEquals(arr, expectedResult);
    }

    @Test
    public void testCopyOf() {
        assertArrayEquals(VectorClockUtils.copyOf(new long[0], 0), new long[0]);
        assertArrayEquals(VectorClockUtils.copyOf(new long[]{1, 2, 3}, 0), new long[0]);
        assertArrayEquals(VectorClockUtils.copyOf(new long[0], 3), new long[]{0, 0, 0});
        assertArrayEquals(VectorClockUtils.copyOf(new long[]{1, 2, 3}, 3), new long[]{1, 2, 3});
        assertArrayEquals(VectorClockUtils.copyOf(new long[]{1, 2, 3}, 2), new long[]{1, 2});
        assertArrayEquals(VectorClockUtils.copyOf(new long[]{1, 2, 3}, 5), new long[]{1, 2, 3, 0, 0});
    }

    @Test
    public void testToString() {
        assertEquals("[]", VectorClockUtils.toString(new long[0]));
        assertEquals("[1,2,3]", VectorClockUtils.toString(new long[]{1, 2, 3}));
    }
/*
    @Test
    public void testSortTidsFramesRetainingMaxFrame() {
        long[] array = new long[0];
        Assert.assertArrayEquals(VectorClockUtils.sortTidsFramesRetainingMaxFrame(array), new long[0]);
        array = new long[]{7, 8, 5, 6, 3, 4, 1, 2};
        Assert.assertArrayEquals(VectorClockUtils.sortTidsFramesRetainingMaxFrame(array), new long[]{1, 2, 3, 4, 5, 6, 7, 8});
        array = new long[]{1, 15, 65, 23, 45, 58, 96, 24, 1, 12, 65, 86, 59, 24};
        Assert.assertArrayEquals(VectorClockUtils.sortTidsFramesRetainingMaxFrame(array), new long[]{1, 15, 45, 58, 59, 24, 65, 86, 96, 24});
    }*/

    @Test
    public void testGetNewFramesCount() {
        performTestGetNewFramesCount(arr(1, 1977), 2, arr(11, 1), 2, 2);
        performTestGetNewFramesCount(arr(), 0, arr(), 0, 0);
        performTestGetNewFramesCount(arr(1, 2, 5, 6), 4, arr(), 0, 4);
        performTestGetNewFramesCount(arr(1, 2, 5, 6), 4, arr(3, 4), 2, 4);
        performTestGetNewFramesCount(arr(1, 2, 3, 5, 5, 6), 4, arr(3, 4), 2, 2);
        performTestGetNewFramesCount(arr(1, 2, 3, 5, 5, 6), 6, arr(3, 4), 2, 4);
        performTestGetNewFramesCount(arr(1, 2, 5, 6), 4, arr(1, 2, 3, 4, 5, 6), 2, 2);
        performTestGetNewFramesCount(arr(1, 2, 5, 6), 4, arr(1, 2, 3, 4, 5, 6), 4, 2);
        performTestGetNewFramesCount(arr(1, 2, 5, 6), 4, arr(1, 2, 3, 4, 5, 6), 6, 0);
        performTestGetNewFramesCount(arr(1, 2, 5, 6, 7, 8), 6, arr(1, 2, 3, 4, 5, 6), 6, 2);
        performTestGetNewFramesCount(arr(1, 2, 5, 6, 9, 10, 0, 0), 6, arr(1, 10, 3, 3, 5, 3, 7, 8, 11, 12, 0, 0, 0, 0), 10, 2);
        performTestGetNewFramesCount(arr(1, 1977, 13, 5, 14, 5, 15, 233, 17, 34968, 20, 3, 0, 0, 0, 0, 0, 0, 0, 0), 12,
                arr(1, 1977, 14, 5, 15, 233, 17, 36367, 19, 273, 20, 3, 0, 0, 0, 0, 0, 0, 0, 0), 12, 2);

    }

    private void performTestGetNewFramesCount(long[] from, int length1, long[] to, int length2, int expectedResult) {
        assertEquals(VectorClockUtils.getNewTidsFramesCount(from, length1, to, length2), expectedResult);
    }

    @Test
    public void testMergeInto() {
        performTestMergeInto(arr(), 0, arr(), 0, arr());
        performTestMergeInto(arr(), 0, arr(1, 2), 2, arr(1, 2));
        performTestMergeInto(arr(1, 1977, 0, 0, 0, 0, 0, 0, 0, 0), 2, arr(11, 1, 0, 0, 0, 0, 0, 0, 0, 0), 2, arr(1, 1977, 11, 1, 0, 0, 0, 0, 0, 0));
        performTestMergeInto(arr(1, 2, 3, 4), 0, arr(), 0, arr());
        performTestMergeInto(arr(1, 2, 3, 4), 2, arr(0, 0, 0, 0), 0, arr(1, 2, 0, 0));
        performTestMergeInto(arr(1, 2, 3, 4), 4, arr(0, 0, 0, 0), 0, arr(1, 2, 3, 4));
        performTestMergeInto(arr(1, 2, 5, 6, 9, 10, 0, 0), 6, arr(1, 10, 3, 3, 5, 3, 7, 8, 11, 12, 0, 0, 0, 0), 10, arr(1, 10, 3, 3, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0));
        performTestMergeInto(arr(1, 2, 5, 6, 9, 10, 13, 14), 8, arr(1, 10, 3, 3, 5, 3, 7, 8, 11, 12, 0, 0, 0, 0), 10, arr(1, 10, 3, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14));
        performTestMergeInto(arr(1, 1977, 13, 5, 14, 5, 15, 233, 17, 34968, 20, 3, 0, 0, 0, 0, 0, 0, 0, 0), 12,
                arr(1, 1977, 14, 5, 15, 233, 17, 36367, 19, 273, 20, 3, 0, 0, 0, 0, 0, 0, 0, 0), 12,
                arr(1, 1977, 13, 5, 14, 5, 15, 233, 17, 36367, 19, 273, 20, 3, 0, 0, 0, 0, 0, 0));
    }

    private void performTestMergeInto(long[] from, int length1, long[] to, int length2, long[] expectedResult) {
        if (length1 > from.length) throw new IllegalArgumentException();
        if (length2 > to.length) throw new IllegalArgumentException();
        VectorClockUtils.mergeInto(from, length1, to, length2, VectorClockUtils.getNewTidsFramesCount(from, length1, to, length2));
        assertArrayEquals(to, expectedResult);
    }

    private static long[] arr(long... elements) {
        return elements;
    }

    private boolean sameClock(long[] cl1, VectorClock cl2) {
        return VectorClockUtils.same(create(cl1), cl2);
    }

    private VectorClock create(long[] clock) {
        return create(clock, clock.length);
    }

    private VectorClock create(long[] clock, int size) {
        final VectorClock vc = new VectorClock();
        vc.clock = clock;
        vc.size = size;
        return vc;
    }

    private VectorClock createRandomClock(int length, int maxTid, int maxFrame) {
        if (maxTid < length) return null;
        final VectorClock vc = new VectorClock();
        for (int i = 0; i < length; i++) {
            long tid;
            int index;
            do {
                tid = random.nextInt(maxTid) + 1;
                index = VectorClockUtils.findTid(vc, tid);
            } while (index >= 0);
            vc.addFrame(-index - 2, tid, random.nextInt(maxFrame) + 1);
        }
        return vc;
    }
}
