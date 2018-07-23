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

import com.devexperts.drd.bootstrap.DataRaceDescription;
import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.bootstrap.DRDEntryPoint;
import com.devexperts.drd.bootstrap.IDataClock;
import com.devexperts.drd.bootstrap.IThreadClock;
import com.devexperts.drd.bootstrap.stats.Counters;
import com.devexperts.drd.bootstrap.stats.Processing;

/**
 * Clock, associated with shared field. Exposes 2 API methods: {@link #read} and {@link #write}, which
 * should be called when some thread performs read or write access to this field, correspondingly.<br/>
 * Instead of storing 2 arrays with clock of last write accessor and of last read accessors, optimised
 * <i>epoch-based</i> internal representation is used.
 * <p/>
 * For more details please refer to
 * <a href='http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.148.2759'>Flanagan & Freund's 2009 article</a>.
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "UnusedDeclaration"})
public class DataClock extends VectorClock implements IDataClock {
    public static final Counter sharedReadsCounter = new Counter();

    private final int ownerId;

    //write epoch
    private long wTid;
    private long wFrame;
    private int wLocation;

    //read epoch
    private long rTid;
    private long rFrame;
    private int rLocation;

    private long[] rLocations;
    private int rLocationsSize;

    /**
     * Indicates if current read state is exclusive (than use read epoch) or shared (use "clock" array, declared in superclass)
     */
    private boolean exclusive;

    public DataClock(int ownerId) {
        super(0);
        DRDEntryPoint.getRegistry().registerDataClock(ownerId);
        this.ownerId = ownerId;
        exclusive = true;
    }

    @Override
    protected void finalize() throws Throwable {
        DRDEntryPoint.getRegistry().unregisterDataClock(ownerId);
        super.finalize();
    }

    /**
     * Must be called when thread with specified vector clock writes associated field.<br/>
     *
     * @param tvc thread clock
     */
    public DataRaceDescription write(IThreadClock tvc, int location, boolean detectWWOnly) {
        //TODO: immediately throw race if workingThread.get() != NO_WORKING_THREAD
        long tid = tvc.getTid();
        acquireLock(tid);
        try {
            if (wTid == tid) {
                wFrame = tvc.currentFrame();
                wLocation = location;
                return null; //same thread => do nothing
            }
            // MSU 2018
            if (wTid > 0 && wFrame >= tvc.getFrame(wTid)) {
                //report write-write race
                return null;//createRaceException(DataRaceDescription.AccessType.WRITE, DataRaceDescription.AccessType.WRITE, tvc, wTid, location, wLocation);
            }
            if (exclusive) {
                if (rTid > 0 && rTid != tid && rFrame >= tvc.getFrame(rTid) && !detectWWOnly) {
                    //report write-read race;
                     // MSU 2018
                    return null;// createRaceException(DataRaceDescription.AccessType.WRITE, DataRaceDescription.AccessType.READ, tvc, rTid, location, rLocation);
                }
                //clear read epoch
                rTid = 0;
                rFrame = 0;
                rLocation = 0;
            } else {
                final long racingTid = VectorClockUtils.checkDataRace((ThreadVectorClock) tvc, this);
                if (racingTid > 0 && !detectWWOnly) {
                    //report write-read race;
                    final int locationIndex = VectorClockUtils.findTid(rLocations, racingTid, rLocationsSize) + 1;
                    final int raceLocation = (int) rLocations[locationIndex];
                     // MSU 2018
                    return null;// createRaceException(DataRaceDescription.AccessType.WRITE, DataRaceDescription.AccessType.READ, tvc, racingTid, location, raceLocation);
                }
                size = 0;
                clock = EMPTY_ARRAY;
                rLocations = EMPTY_ARRAY;
                rLocationsSize = 0;
                exclusive = true;
            }
            wTid = tid;
            wFrame = tvc.currentFrame();
            wLocation = location;
            return null;
        } finally {
            releaseLock();
        }
    }

    /**
     * Must be called when thread with specified vector clock reads associated field.<br/>
     *
     * @param tvc thread clock
     */
    public DataRaceDescription read(IThreadClock tvc, int location, boolean detectWWOnly) {
        //TODO: immediately throw race if workingThread.get() != NO_WORKING_THREAD and working thread is writing
        long tid = tvc.getTid();
        acquireLock(tid);
        try {
            if (tid == rTid) {
                rFrame = tvc.currentFrame();
                rLocation = location;
                return null; //same thread => do nothing.
            }
            if (wTid > 0 && wTid != tid && wFrame >= tvc.getFrame(wTid) && (tvc.getFrame2(wTid) < 0) && !detectWWOnly) {
                //report read-write race
                return createRaceException(DataRaceDescription.AccessType.READ, DataRaceDescription.AccessType.WRITE, tvc, wTid, location, wLocation);
            }
            //2 reads can't race => no more checks in this method
            final long tCurrentFrame = tvc.currentFrame();
            if (exclusive) {
                if (rFrame <= tvc.getFrame(rTid)) {
                    //clock of current thread happens-after epoch of last reader
                    // => this read is exclusive for current thread. simply update read epoch
                    rFrame = tCurrentFrame;
                    rTid = tid;
                    rLocation = location;
                } else {
                    //switch state to shared, init array of read clocks, clear read epoch.
                    //Only ~ 0.1% calls are expected to reach this point
                    exclusive = false;
                    sharedReadsCounter.increment();
                    clock = new long[4];
                    size = 4;
                    rLocations = new long[4];
                    rLocationsSize = 4;
                    //array should be sorted by tid asc(superclass invariant)
                    if (tid < rTid) {
                        clock[0] = tid;
                        clock[1] = tCurrentFrame;
                        clock[2] = rTid;
                        clock[3] = rFrame;
                        rLocations[0] = tid;
                        rLocations[1] = location;
                        rLocations[2] = rTid;
                        rLocations[3] = rLocation;
                    } else {
                        clock[0] = rTid;
                        clock[1] = rFrame;
                        clock[2] = tid;
                        clock[3] = tCurrentFrame;
                        rLocations[0] = rTid;
                        rLocations[1] = rLocation;
                        rLocations[2] = tid;
                        rLocations[3] = location;
                    }
                    //clear read epoch
                    rTid = 0;
                    rFrame = 0;
                    rLocation = 0;
                }
            } else {
                setFrame(tid, tCurrentFrame);
                int index = VectorClockUtils.findTid(rLocations, tid, rLocationsSize);
                if (index >= 0) {
                    rLocations[index + 1] = location;
                } else {
                    final int insertIndex = -index - 2;
                    if (rLocationsSize <= rLocations.length - 2) {
                        //there is enough place in existing array
                        VectorClockUtils.addFrameAndShift(rLocations, tid, location, insertIndex, rLocationsSize);
                        rLocationsSize += 2;
                    } else {
                        sharedReadsCounter.increment();
                        long[] rLoc = new long[rLocations.length * 2];
                        System.arraycopy(rLocations, 0, rLoc, 0, insertIndex);
                        rLoc[insertIndex] = tid;
                        rLoc[insertIndex + 1] = location;
                        System.arraycopy(rLocations, insertIndex, rLoc, insertIndex + 2, rLocations.length - insertIndex);
                        rLocations = rLoc;
                        rLocationsSize += 2;
                    }
                }
            }
            return null;
        } finally {
            releaseLock();
        }
    }

    @Override
    public int getRealSizeUnprotected() {
        //4 longs: rTid, rFrame, wTid, wFrame
        return size == 0 ? 4 : size;
    }

    @Override
    public int getReservedSizeUnprotected() {
        //4 longs: rTid, rFrame, wTid, wFrame
        return clock.length == 0 ? 4 : clock.length;
    }

    public long[] getReadFrames() {
        if (exclusive) {
            return new long[]{rTid, rFrame};
        } else {
            long[] result = new long[size];
            System.arraycopy(clock, 0, result, 0, size);
            return result;
        }
    }

    public long[] getWriteFrames() {
        return new long[]{wTid, wFrame};
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("W:[").append(wTid).append(":").append(wFrame).append("] ");
        sb.append("R:");
        if (exclusive) sb.append("[").append(rTid).append(":").append(rFrame).append("]");
        else {
            for (int i = 0; i < size / 2; i++) {
                sb.append("[").append(clock[2 * i]).append(":").append(clock[2 * i + 1]).append("]");
            }
        }
        sb.append("; exclusive = ").append(exclusive);
        sb.append("}");
        return sb.toString();
    }

/*    public String detailedToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("wTid = ").append(wTid).append("\n");
        sb.append("wFrame = ").append(wFrame).append("\n");
        sb.append("wLocation = ").append(wLocation).append("\n");
        sb.append("rTid = ").append(rTid).append("\n");
        sb.append("rFrame = ").append(rFrame).append("\n");
        sb.append("rLocation = ").append(rLocation).append("\n");
        sb.append("rLocations = ").append(Arrays.toString(rLocations)).append("\n");
        sb.append("rLocationsSize = ").append(rLocationsSize).append("\n");
        sb.append("exclusive = ").append(exclusive).append("\n");
        return sb.toString();
    }*/

    private DataRaceDescription createRaceException(DataRaceDescription.AccessType currentAccess, DataRaceDescription.AccessType racingAccess, IThreadClock tvc, long racingTid, int location, int raceLocation) {
        if (DRDEntryPoint.getRegistry().getRaceHistory().shouldReportRace(location, raceLocation)) {
            DRDEntryPoint.getStatistics().increment(Counters.RACE, Processing.PROCESSED, "");
            return new DataRaceDescription(currentAccess, racingAccess, tvc, racingTid, this, location, raceLocation);
        } else {
            DRDEntryPoint.getStatistics().increment(Counters.RACE, Processing.IGNORED, "");
            return null;
        }
    }
}
