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
import java.io.*;
import java.util.ArrayList;
import java.util.List;

class Utils {
    private Utils() {
    }

    private interface LineReader {
        String readLine() throws IOException;
    }

    static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                //skip
            }
        }
    }

    /**
     * Reads next race from stream
     *
     * @return races
     * @throws JAXBException if JAXB error occurs
     * @throws IOException   if IO error occurs
     */
    static List<Race> readRaces(InputStream is, Unmarshaller u) throws JAXBException, IOException {
        List<Race> result = new ArrayList<Race>();
        LineReader reader = new InputStreamLineReader(is);
        Race race;
        while ((race = readRace(reader, u)) != null) {
            result.add(race);
        }
        return result;
    }

    /**
     * Reads next race from file from current position
     *
     * @return next race or null, if no more races found
     * @throws JAXBException if JAXB error occurs
     * @throws IOException   if IO error occurs
     */
    static Race readRace(RandomAccessFile raf, Unmarshaller u) throws JAXBException, IOException {
        return readRace(new RandomAccessFileReader(raf), u);
    }

    private static Race readRace(LineReader reader, Unmarshaller unmarshaller) throws JAXBException, IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            sb.append(line);
            if (line.equalsIgnoreCase("</Race>")) {
                return (Race) unmarshaller.unmarshal(new StringReader(sb.toString()));
            }
        }
        return null;
    }

    private static final class RandomAccessFileReader implements LineReader {
        private final RandomAccessFile raf;

        private RandomAccessFileReader(RandomAccessFile raf) {
            this.raf = raf;
        }

        @Override
        public String readLine() throws IOException {
            return raf.readLine();
        }
    }

    private static final class InputStreamLineReader implements LineReader {
        private final BufferedReader reader;

        private InputStreamLineReader(InputStream is) {
            this.reader = new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public String readLine() throws IOException {
            return reader.readLine();
        }
    }
}
