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

import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.IThreadClock;

/**
 * Thread vector clock. Not thread-safe. Should be used only by owner thread.
 * TODO: should be located directly inside java.lang.Thread class
 * */
public class ThreadVectorClock extends VectorClock implements IThreadClock {
    /**
     * Tid of owner thread
     */
    public final long tid;
    private int index;
    /**
     * Reference to last data clock that were merged from/to this thread clock
     */
    VectorClock lastLock = null;

    public static void init() {
        DRDLogger.log("Thread clock init at " + System.currentTimeMillis() + " !");
    }

    public ThreadVectorClock() {
        clock[0] = tid = Thread.currentThread().getId();
        clock[1] = 1;
        size = 2;
        index = 1;
    }

    public long getTid() {
        return tid;
    }

    /**
     * Increments frame of current tid in O(1).
     */
    public void tick() {
        clock[index]++;
    }

    public long currentFrame() {
        return clock[index];
    }

    @Override
    protected void addFrame(int index, long tid, long frame) {
        super.addFrame(index, tid, frame);
        if (index <= this.index) {
            this.index += 2;
        }
    }

    void updateCachedIndex() {
        index = VectorClockUtils.findTid(this, tid) + 1;
        if (index < 0) throw new RuntimeException("index : " + index + " clock : " + this);
    }

    @Override
    public void checkGeneration() {
        int gen = generation;
        super.checkGeneration();
        if (gen != generation) updateCachedIndex();
    }

    @Override
    public String toString() {
        return super.toString() + "; tid = " + tid;
    }

    @Override
    protected void acquireLock(long tid) {
        //do nothing
    }

    @Override
    protected void releaseLock() {
        //do nothing
    }
}
