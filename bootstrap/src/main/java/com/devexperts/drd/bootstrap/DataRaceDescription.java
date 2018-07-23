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

package com.devexperts.drd.bootstrap;

/**
 * Internal description of data race
 */
public class DataRaceDescription {
    public enum RaceTarget {
        FIELD, OBJECT
    }

    public enum AccessType {
        READ, WRITE
    }

    public final AccessType currentAccess;
    public final AccessType racingAccess;
    public final int currentLocationId;
    public final int racingLocationId;
    public final long racingTid;
    public final IThreadClock currentThreadVC;
    public final IDataClock targetVC;

    public DataRaceDescription(AccessType currentAccess, AccessType racingAccess, IThreadClock currentThreadVC, long racingTid,
                               IDataClock targetVC, int currentLocationId, int racingLocationId) {
        this.currentAccess = currentAccess;
        this.racingAccess = racingAccess;
        this.currentThreadVC = currentThreadVC;
        this.racingTid = racingTid;
        this.targetVC = targetVC;
        this.currentLocationId = currentLocationId;
        this.racingLocationId = racingLocationId;
    }
}
