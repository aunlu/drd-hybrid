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

import com.devexperts.drd.race.DataClock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlType(name = "dataClock", propOrder = {"readFrames", "writeFrames"})
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class DataClockImpl implements DataClock {
    @XmlList
    private long[] readFrames;
    @XmlList
    private long[] writeFrames;

    public DataClockImpl(long[] readFrames, long[] writeFrames) {
        this.readFrames = readFrames;
        this.writeFrames = writeFrames;
    }

    public DataClockImpl() {
    }

    public long[] getReadFrames() {
        return readFrames;
    }

    public long[] getWriteFrames() {
        return writeFrames;
    }

    public List<Frame> getFrames() {
        List<DataClock.Frame> result = new ArrayList<DataClock.Frame>();
        for (int i = 0; i < readFrames.length; i += 2) {
            result.add(new DataClock.Frame(readFrames[i], readFrames[i + 1], true));
        }
        for (int i = 0; i < writeFrames.length; i += 2) {
            result.add(new DataClock.Frame(writeFrames[i], writeFrames[i + 1], false));
        }
        return result;
    }

    @Override
    public String toString() {
        return "DataClock{" +
                "readFrames=" + Arrays.toString(readFrames) +
                ", writeFrames=" + Arrays.toString(writeFrames) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataClockImpl dataClock = (DataClockImpl) o;

        if (!Arrays.equals(readFrames, dataClock.readFrames)) return false;
        return Arrays.equals(writeFrames, dataClock.writeFrames);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(readFrames);
        result = 31 * result + Arrays.hashCode(writeFrames);
        return result;
    }
}
