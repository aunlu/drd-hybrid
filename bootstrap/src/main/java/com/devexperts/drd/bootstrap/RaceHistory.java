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

public interface RaceHistory {
    public boolean shouldReportRace(int location, int raceLocation);

    public void trackForeignCall(Object o, int ownerId, int location);

    public ThreadRaceInfo getFieldAccess(int ownerId, int nameId, long tid);

    public ThreadRaceInfo getObjectAccess(Object o, int ownerId, long tid);

    public void trackFieldAccesses(int ownerId, int nameId, int location);
}
