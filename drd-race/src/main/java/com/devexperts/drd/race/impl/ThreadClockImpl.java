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

package com.devexperts.drd.race.impl;

import com.devexperts.drd.race.ThreadClock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlType(name = "threadClock", propOrder = {"liveFrames", "deadFrames"})
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ThreadClockImpl implements ThreadClock {
    @XmlList
    private long[] liveFrames;
    @XmlList
    private long[] deadFrames;

    public ThreadClockImpl(long[] liveFrames, long[] deadFrames) {
        this.liveFrames = liveFrames;
        this.deadFrames = deadFrames;
    }

    public ThreadClockImpl() {
    }

    /**
     * @return Frames of live threads: [tid1, frame1, ... tidN, frameN]
     */
    long[] getLiveFrames() {
        return liveFrames;
    }

    /**
     * @return Frames of dead threads: [tid1, frame1, ... tidN, frameN]
     */
    long[] getDeadFrames() {
        return deadFrames;
    }

    @Override
    public String toString() {
        return "VectorClock{" +
                "liveFrames=" + Arrays.toString(liveFrames) +
                ", deadFrames=" + Arrays.toString(deadFrames) +
                '}';
    }

    public List<Frame> getFrames() {
        List<Frame> result = new ArrayList<Frame>();
        for (int i = 0; i < liveFrames.length; i+=2) {
            result.add(new Frame(liveFrames[i], liveFrames[i+1], true));
        }
        for (int i = 0; i < deadFrames.length; i+=2) {
            result.add(new Frame(deadFrames[i], deadFrames[i+1], false));
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreadClockImpl that = (ThreadClockImpl) o;

        if (!Arrays.equals(liveFrames, that.liveFrames)) return false;
        return Arrays.equals(deadFrames, that.deadFrames);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(liveFrames);
        result = 31 * result + Arrays.hashCode(deadFrames);
        return result;
    }
}
