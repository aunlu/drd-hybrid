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

import com.devexperts.drd.agent.Generations;
import com.devexperts.drd.bootstrap.UnsafeHolder;
import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.IVectorClock;

/**
 * Core class of detector, representing vector clock.<br/>
 * Clock are stored in an array clock[tid_1, frame_1, tid_2, frame_2, .., tid_(size/2), frame_(size/2), 0, .. 0]. <br/>
 * Initially size is 0, further it is stored in "size" field. Size is always even and &lt;= array length.<br/>
 * When array gets filled, it is resized (length of new array would be old length * 2).<br/>
 * <b>Array is always sorted by tid</b> - i.e, <code> for any i = 0.. (size/2 - 1) array[i] < array[i+2]</code>.
 * This fact is always used when searching for frame or merging two clock.
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "UnusedDeclaration"})
public class VectorClock implements IVectorClock {
    /*public static final Profiler creationProfiler = new Profiler(100);
    public static final Profiler deathProfiler = new Profiler(100);*/
    private static final int NO_WORKING_THREAD = -1;
    private static final int INIT_SIZE = 10;
    private static final long workingThread_offset;

    public static final Counter resizeProfiler = new Counter();
    public static final Counter deadResizeProfiler = new Counter();
    public static final Counter hardGenerationUpdateCounter = new Counter();
    public static final Counter lightGenerationUpdateCounter = new Counter();
    public static final Counter zeroGenCounter = new Counter();
    public static final Counter yieldCounter = new Counter();

    static {
        try {
            workingThread_offset = UnsafeHolder.UNSAFE.objectFieldOffset(VectorClock.class.getDeclaredField("workingThread"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            DRDLogger.error(e);
            throw new RuntimeException();
        }
    }

    //this field is updated via Unsafe.xxxLong
    @SuppressWarnings("FieldCanBeLocal")
    private volatile long workingThread = NO_WORKING_THREAD;
    /**
     * Size is always even. For each i in 0.. clock.length/2 - 1 clock[2*i+1] is frame for thread with tid==clock[2*i]
     */
    long[] clock;
    int size;
    protected int generation;
    long[] deadClock;

    /**
     * Creates new clock of initial size.
     */
    public VectorClock() {
        this(INIT_SIZE * 2);
    }

    protected VectorClock(int size) {
        clock = new long[size];
        this.size = 0;
        deadClock = EMPTY_ARRAY;
    }

    /**
     * copy constructor
     *
     * @param vc clock to copy from
     */
    public VectorClock(VectorClock vc) {
        this.size = vc.size;
        this.clock = new long[vc.clock.length];
        this.deadClock = vc.deadClock;
        System.arraycopy(vc.clock, 0, clock, 0, size);
        generation = vc.generation;
    }

    public int getRealSizeUnprotected() {
        return size;
    }

    public int getReservedSizeUnprotected() {
        return clock.length;
    }

    public long[] getLiveFrames() {
        long[] res = new long[size];
        System.arraycopy(clock, 0, res, 0, size);
        return res;
    }

    public long[] getDeadFrames() {
        long[] res = new long[deadClock.length];
        System.arraycopy(deadClock, 0, res, 0, deadClock.length);
        return res;
    }

    public int getDeadClockSize() {
        return deadClock.length;
    }

    /**
     * Sets frame for specified tid to specified value.
     *
     * @param tid   target thread id
     * @param frame frame to set
     */
    protected void setFrame(long tid, long frame) {
        int index = VectorClockUtils.findTid(this, tid);
        if (index >= 0) {
            clock[index + 1] = frame;
        } else addFrame(-index - 2, tid, frame);
    }

    /**
     * Retrieves frame for specified tid. If such frame doesn't exist, returns 0.
     *
     * @param tid target thread id
     * @return frame for specified thread
     */
    public long getFrame(long tid) {
        int index = VectorClockUtils.findTid(this, tid);
        if (index >= 0) return clock[index + 1];
        else {
            index = VectorClockUtils.findTid(deadClock, tid, deadClock.length);
            return index >= 0 ? deadClock[index + 1] : 0;
        }
    }

    // MSU 2018
/*     */   public int getFrame2(long tid)
/*     */   {
/* 158 */     return VectorClockUtils.findTid(this, tid);
/* 159 */   } 
    /**
     * Internal method for adding frame at specified index. Performs no checks of preserving internal invariants of clock.
     * Typically index to insert is received via {@link VectorClockUtils#findTid}.
     *
     * @param index index at which insert tid and his frame
     * @param tid   tid to insert
     * @param frame frame to insert
     */
    protected void addFrame(int index, long tid, long frame) {
        if (size <= clock.length - 2) {
            //there is enough place in existing array
            VectorClockUtils.addFrameAndShift(clock, tid, frame, index, size);
            size += 2;
        } else {
            //resize
            resizeProfiler.increment();
            long[] cl = new long[clock.length * 2];
            System.arraycopy(clock, 0, cl, 0, index);
            cl[index] = tid;
            cl[index + 1] = frame;
            System.arraycopy(clock, index, cl, index + 2, clock.length - index);
            clock = cl;
            size += 2;
        }
    }

    protected void acquireLock(long tid) {
        if (tid != workingThread) {
            while (!UnsafeHolder.UNSAFE.compareAndSwapLong(this, workingThread_offset, NO_WORKING_THREAD, tid)) {
                yieldCounter.increment();
                //do nothing
                Thread.yield();
            }
        }
    }

    protected void releaseLock() {
        UnsafeHolder.UNSAFE.putOrderedLong(this, workingThread_offset, NO_WORKING_THREAD);
    }


    private void incrementGeneration(long tid) {
        int cacheIndex = VectorClockUtils.findTid(clock, tid, size);
        if (cacheIndex > 0) {
            long frame = clock[cacheIndex + 1];
            VectorClockUtils.removeFrameAndShift(clock, cacheIndex, size);
            size -= 2;
            int insertIndex = -VectorClockUtils.findTid(deadClock, tid, deadClock.length) - 2;
            if (insertIndex >= 0) {
                deadClock = VectorClockUtils.copyOf(deadClock, deadClock.length + 2);
                VectorClockUtils.addFrameAndShift(deadClock, tid, frame, insertIndex, deadClock.length - 2);
            }
        }
        generation++;
    }

    protected void checkGeneration() {
        int newGen = Generations.generation;
        if (generation == newGen) return;
        if (generation == 0) {
            zeroGenCounter.increment();
        }
        if (generation == newGen - 1) {
            //optimise for case of generation increment
            lightGenerationUpdateCounter.increment();
            incrementGeneration(Generations.dead[generation]);
            return;
        }
        hardGenerationUpdateCounter.increment();
        /*TODO: optimise -"toCache" array is redundant*/
        long[] diff = Generations.getDiff(generation);
        long[] toCache = new long[diff.length * 2];
        int i = 0;
        int j = 0;
        int index = 0;
        int iMax = size;
        int jMax = diff.length;
        while (i < iMax && j < jMax) {
            if (clock[i] > diff[j]) j++;
            else if (clock[i] < diff[j]) {
                i += 2;
            } else {
                toCache[index++] = clock[i];
                clock[i++] = 0;
                toCache[index++] = clock[i];
                clock[i++] = 0;
                j++;
            }
        }
        if (index > 0) {
            deadResizeProfiler.increment();
            size = VectorClockUtils.removeZeros(clock);
            long[] newDeadClock = new long[deadClock.length + index];
            int newLength = VectorClockUtils.mergeSortedClocks(deadClock, deadClock.length, toCache, index, newDeadClock);
            if (newLength != newDeadClock.length) {
                //ouch
                String msg = "We were merging deadclock of length " + deadClock.length + "(" + VectorClockUtils.toString(deadClock) +
                        ") and toCache clock of length = " + toCache.length + "(" + VectorClockUtils.toString(toCache) +
                        ") expecting result of length " + newDeadClock.length + " but received result of length " + newLength + ": " +
                        VectorClockUtils.toString(newDeadClock);
                throw new IllegalStateException(msg);
            }
            deadClock = newDeadClock;
        }
        //VectorClockUtils.assureCompliant(this);
        generation = newGen;
    }

    /**
     * @return string representation of this clock.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder("Live: ");
        for (int i = 0; i < size / 2; i++) {
            sb.append("[").append(clock[2 * i]).append(":").append(clock[2 * i + 1]).append("]");
        }
        sb.append(" size = ").append(size).append("; ");
        sb.append("Dead: ");
        for (int i = 0, length = deadClock.length; i < length / 2; i++) {
            sb.append("[").append(deadClock[2 * i]).append(":").append(deadClock[2 * i + 1]).append("]");
        }
        return sb.toString();
    }
}
