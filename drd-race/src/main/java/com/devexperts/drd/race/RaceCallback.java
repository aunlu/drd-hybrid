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

package com.devexperts.drd.race;

import com.devexperts.drd.race.io.RaceIOException;

import java.util.List;

/**
 * The callback that receives information about changes in races file
 */
public interface RaceCallback {
    /**
     * Executed when new races are available
     * @param races list of new races
     */
    void newData(List<Race> races);

    /**
     * Executed on file deletion
     */
    void deleted();

    /**
     * Executed on file creation
     */
    void created();

    /**
     * Executed on error
     * @param e error
     */
    void error(RaceIOException e);
}
