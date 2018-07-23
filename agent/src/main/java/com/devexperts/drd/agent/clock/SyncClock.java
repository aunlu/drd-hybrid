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
import com.devexperts.drd.bootstrap.ISyncClock;
import com.devexperts.drd.bootstrap.IThreadClock;

public class SyncClock extends VectorClock implements ISyncClock {
    public static final Counter optimisedAcquireProfiler = new Counter();
    public static final Counter optimisedReleaseProfiler = new Counter();
    public static final Counter fullAcquireProfiler = new Counter();
    public static final Counter fullReleaseProfiler = new Counter();
    public static final Counter twoWayProfiler = new Counter();
    long lastThread;

    public SyncClock(VectorClock clock) {
        super(clock);
    }

    public SyncClock() {
        super();
    }

    public void loadFrom(IThreadClock tvc) {
        //TODO bad cast
        ThreadVectorClock tc = (ThreadVectorClock) tvc;
        acquireLock(tc.tid);
        try {
            loadFromInternal(tc);
        } finally {
            releaseLock();
        }
    }

    public void loadTo(IThreadClock tvc) {
        //TODO bad cast
        ThreadVectorClock tc = (ThreadVectorClock) tvc;
        acquireLock(tc.tid);
        try {
            loadToInternal(tc);
        } finally {
            releaseLock();
        }
    }

    public void loadTwoWay(IThreadClock tvc) {
        //TODO bad cast
        ThreadVectorClock tc = (ThreadVectorClock) tvc;
        acquireLock(tc.tid);
        try {
            twoWayProfiler.increment();
            //TODO optimise
            loadToInternal(tc);
            loadFromInternal(tc);
        } finally {
            releaseLock();
        }
    }

    private void loadToInternal(ThreadVectorClock tvc) {
        if (lastThread == tvc.tid && tvc.lastLock == this) {
            //optimize: do nothing
            optimisedAcquireProfiler.increment();
        } else {
            fullAcquireProfiler.increment();
            tvc.checkGeneration();
            checkGeneration();
            VectorClockUtils.load(this, tvc);
            tvc.updateCachedIndex();
            tvc.lastLock = this;
        }
        //VectorClockUtils.assureCompliant(this);
    }

    private void loadFromInternal(ThreadVectorClock tvc) {
        if (lastThread == tvc.tid) {
            //optimize: load only current frame
            optimisedReleaseProfiler.increment();
            setFrame(tvc.tid, tvc.currentFrame());
        } else {
            fullReleaseProfiler.increment();
            tvc.checkGeneration();
            checkGeneration();
            VectorClockUtils.load(tvc, this);
            lastThread = tvc.tid;
        }
        //VectorClockUtils.assureCompliant(this);
    }
}
