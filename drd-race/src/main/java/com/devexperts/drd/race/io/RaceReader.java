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

package com.devexperts.drd.race.io;

import com.devexperts.drd.race.Race;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads new races from file. Correctly processes file deletion/recreation.
 */
class RaceReader {
    private final String file;
    private long position;
    private long timestamp;
    private final Unmarshaller u;

    RaceReader(String file, Unmarshaller u) {
        this.file = file;
        this.u = u;
    }

    /**
     * Reads races that appeared in file since last read. If file was recreated, reads all races from it.
     *
     * @return new races
     * @throws IOException   if IO error occurs
     * @throws JAXBException if JAXB error occurs
     */
    List<Race> read() throws IOException, JAXBException {
        List<Race> result = new ArrayList<Race>();
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        if (timestamp > 0) {
            //check if file is the same
            raf.seek(0);
            Race firstRace = Utils.readRace(raf, u);
            if (firstRace == null) {
                position = 0;
                timestamp = 0;
                return result;
            } else {
                long time = firstRace.getTime().getTime();
                if (time > timestamp) {
                    timestamp = time;
                    result.add(firstRace);
                    position = raf.getFilePointer();
                } else {
                    raf.seek(position); //old file, read from previously saved position
                }
            }
        }
        Race race;
        while ((race = Utils.readRace(raf, u)) != null) {
            result.add(race);
            position = raf.getFilePointer();
        }
        if (timestamp == 0 && result.size() > 0) {
            timestamp = result.get(0).getTime().getTime();
        }
        Utils.closeQuietly(raf);
        return result;
    }
}
