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

import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.bootstrap.DRDLogger;

public class VectorClockUtils {
    public static final Counter cachedCounter = new Counter();
    public static final Counter copyCounter = new Counter();
    public static final Counter sameCounter = new Counter();

    public static final Counter liveAllocCounter = new Counter();
    public static final Counter liveNotAllocCounter = new Counter();

    /**
     * Checks if two clock contains same tids and same frames for them
     */
    static boolean same(VectorClock vc1, VectorClock vc2) {
        if (vc1.size != vc2.size) {
            return false;
        }
        for (int i = 0; i < vc1.size; i++) {
            if (vc1.clock[i] != vc2.clock[i]) return false;
        }
        return true;
    }

    /**
     * Searches vector clock for the specified tid using the
     * binary search algorithm. Assumes, that internal array of clock is always sorted by tids.
     * If it is not sorted, the results are undefined.
     *
     * @param vc  clock
     * @param tid the tid to be searched for
     * @return index of the search key, if it is contained in the array;
     * otherwise, <tt>(-(<i>insertion point</i>) - 2)</tt>.  The
     * <i>insertion point</i> is defined as the point at which the
     * key would be inserted into the array: the index of the first
     * tid greater than the key, or <tt>a.length</tt> if all
     * elements in the array are less than the specified key.  Note
     * that this guarantees that the return value will be &gt;= 0 if
     * and only if the key is found.
     */
    static int findTid(VectorClock vc, long tid) {
        return findTid(vc.clock, tid, vc.size);
    }

    static int findTid(long[] clock, long tid, int to) {
        int low = 0;
        int high = to - 2;

        while (low <= high) {
            int mid = (low + high >>> 1) & -2; //shift right and set last bit to 0
            long midVal = clock[mid];

            if (midVal < tid) {
                low = mid + 2;
            } else if (midVal > tid) {
                high = mid - 2;
            } else {
                return mid; // key found
            }
        }
        return -(low + 2);  // key not found.
    }

    public static void addFrameAndShift(long[] clock, long tid, long frame, int index, int size) {
        if (index < 0) {
            throw new IllegalArgumentException("Index: " + index + " size : " + size + " tid : " + tid + " frame : " + frame +
                    " clock: " + toString(clock));
        }
        System.arraycopy(clock, index, clock, index + 2, size - index);
        clock[index] = tid;
        clock[index + 1] = frame;
    }

    static void removeFrameAndShift(long[] clock, int index, int size) {
        System.arraycopy(clock, index + 2, clock, index, size - 2 - index);
        clock[size - 2] = 0;
        clock[size - 1] = 0;
    }

    static int removeZeros(long[] clock) {
        int zeroIndex = -1;
        for (int i = 0; i < clock.length; i++) {
            if (clock[i] == 0) {
                if (zeroIndex == -1) zeroIndex = i;
            } else if (zeroIndex != -1) {
                clock[zeroIndex++] = clock[i];
                clock[i] = 0;
            }
        }
        return zeroIndex == -1 ? clock.length : zeroIndex;
    }

    static void load(VectorClock from, VectorClock to) {
        //TODO refactor. It works miraculously :)
        removeFrames(to, from.deadClock);
        loadLiveComponents(from, to);
        if (from.deadClock != to.deadClock) {
            if (!checkIn(to.deadClock, from.deadClock)) {
                final long[] dc = new long[from.deadClock.length + to.deadClock.length];
                int newSize = mergeSortedClocks(from.deadClock, from.deadClock.length, to.deadClock, to.deadClock.length, dc);
                //TODO copyOf contains implicit array copy - remove it!
                to.deadClock = newSize == dc.length ? dc : copyOf(dc, newSize);
                copyCounter.increment();
            } else {
                to.deadClock = from.deadClock;
                cachedCounter.increment();
            }
        } else {
            sameCounter.increment();
        }
    }

    static void removeFrames(VectorClock vc, long[] toRemove) {
        long[] clock = vc.clock;
        int i = 0, j = 0;
        int size = clock.length;
        while (i < size && j < toRemove.length) {
            if (clock[i] == 0) {
                break; //we've reached end
            }
            if (clock[i] > toRemove[j]) j += 2;
            else if (clock[i] < toRemove[j]) i += 2;
            else {
                clock[i++] = 0;
                clock[i++] = 0;
                j += 2;
            }
        }
        vc.size = removeZeros(clock);
    }

    /**
     * @param tvc current thread clock
     * @param fvc field clock
     * @return true iff tvc races with some other thread, whose info is stored in fvc
     */
    static long checkDataRace(VectorClock tvc, VectorClock fvc) {
        long currentTid = Thread.currentThread().getId();
        int i = 0, j = 0;
        while (i < tvc.size && j < fvc.size) {
            long ftid = fvc.clock[j];
            while (i < tvc.size && ftid > tvc.clock[i]) {
                i += 2;
            }
            if (i == tvc.size) return ftid;
            long ttid = tvc.clock[i];
            if (ftid < ttid) return ftid;
            if (ttid == ftid && ttid != currentTid && tvc.clock[i + 1] <= fvc.clock[j + 1]) return ftid;
            j += 2;
        }
        return -1;
    }

    private static void loadLiveComponents(VectorClock from, VectorClock to) {
        int newTidsFramesCount = getNewTidsFramesCount(from.clock, from.size, to.clock, to.size);
        final int newSize = to.size + newTidsFramesCount;
        if (newSize < to.clock.length) {
            mergeInto(from.clock, from.size, to.clock, to.size, newTidsFramesCount);
            liveNotAllocCounter.increment();
/*            if (!verifyClocks(to.clock, newSize)) {
                throw new RuntimeException("Failed merge into : " + toString(to.clock) + " size : " + newSize);
            }*/
        } else {
            liveAllocCounter.increment();
            long[] newClock = new long[getNewLength(newSize)];
            //DRDLogger.error(from + " merge to " + to + "; new size : " + newClock.length);
            mergeSortedClocks(from.clock, from.size, to.clock, to.size, newClock);
            to.clock = newClock;
/*            if (!verifyClocks(to.clock, newSize)) {
                throw new RuntimeException("Failed merge : " + toString(to.clock) + " size : " + newSize);
            }*/
        }
        to.size = newSize;
        //DRDLogger.error(to);
    }

    private static int getNewLength(int size) {
        if (size < 5) {
            return size * 2;
        }
        return size + 4;
    }

    public static int mergeSortedClocks(long[] cl1, int length1, long[] cl2, int length2, long[] res) {
        try {
            int i = 0, j = 0, k = 0;
            while (i < length1 && j < length2) {
                if (cl1[i] < cl2[j]) {
                    res[k++] = cl1[i++];
                    res[k++] = cl1[i++];
                } else if (cl1[i] > cl2[j]) {
                    res[k++] = cl2[j++];
                    res[k++] = cl2[j++];
                } else {
                    res[k++] = cl2[j++];
                    res[k++] = Math.max(cl1[++i], cl2[j++]);
                    i++;
                }
            }
            if (i < length1) {
                System.arraycopy(cl1, i, res, k, length1 - i);
                k += length1 - i;
            } else if (j < length2) {
                System.arraycopy(cl2, j, res, k, length2 - j);
                k += length2 - j;
            }
            return k;
        } catch (Throwable e) {
            DRDLogger.error("Fatal: mergeSortedClocks failed for : " + toString(cl1) + " " + length1 + "; " + toString(cl2) + " " + length2
                    + "; " + toString(res), e);
            throw new RuntimeException();
        }
    }

    static int getNewTidsFramesCount(long[] from, int length1, long[] to, int length2) {
        int count = 0;
        int i = 0, j = 0;
        while (i < length1 && j < length2) {
            if (from[i] < to[j]) {
                count += 2;
                i += 2;
            } else if (from[i] > to[j]) {
                j += 2;
            } else {
                i += 2;
                j += 2;
            }
        }
        if (i < length1) {
            count += length1 - i;
        }
        return count;
    }

    /**
     * generates no garbage
     */
    static void mergeInto(long[] from, int length1, long[] to, int length2, int shift) {
        int i = length1;
        int j = length2;
        while (i > 0 && j > 0) {
            if (to[j - 2] > from[i - 2]) {
                System.arraycopy(to, j - 2, to, j - 2 + shift, 2);
                j -= 2;
            } else if (to[j - 2] == from[i - 2]) {
                if (from[i - 1] > to[j - 1]) {
                    to[j - 1] = from[i - 1];
                }
                System.arraycopy(to, j - 2, to, j - 2 + shift, 2);
                i -= 2;
                j -= 2;
            } else { //to[j-1] < from[i-1]
                System.arraycopy(from, i - 2, to, j - 2 + shift, 2);
                shift -= 2;
                i -= 2;
            }
        }
        if (i > 0) {
            System.arraycopy(from, 0, to, 0, i);
        }
    }

    /**
     * @param c1
     * @param c2
     * @return true iff c1 is in c2
     */
    static boolean checkIn(long[] c1, long[] c2) {
        int iMax = c1.length;
        int jMax = c2.length;
        if (iMax > jMax) {
            return false;
        }
        int i = 0;
        int j = 0;
        while (i < iMax && j < jMax) {
            if (c1[i] < c2[j]) return false;
            else if (c1[i] > c2[j]) j += 2;
            else {
                if (c1[i + 1] > c2[j + 1]) return false;
                else {
                    i += 2;
                    j += 2;
                }
            }
        }
        return i == iMax;
    }

    static long[] copyOf(long[] array, int length) {
        long[] copy = new long[length];
        System.arraycopy(array, 0, copy, 0, length < array.length ? length : array.length);
        return copy;
    }

    static void assureCompliant(VectorClock vc) {
        if (vc.deadClock == null || vc.deadClock.length == 0) {
            return;
        }
        int i = 0;
        int j = 0;
        int size = vc.size;
        int size2 = vc.deadClock.length;
        while (i < size && j < size2) {
            if (vc.clock[i] < vc.deadClock[j]) i += 2;
            else if (vc.clock[i] > vc.deadClock[j]) j += 2;
            else throw new IllegalStateException("Clock not compliant : " + vc);
        }
    }

    static String toString(long[] a) {
        StringBuilder sb = new StringBuilder("[");
        if (a.length > 0) {
            for (long l : a) {
                sb.append(l).append(",");
            }
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.append("]").toString();
    }
}
