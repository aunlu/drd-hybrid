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

package com.devexperts.drd.agent;

import com.devexperts.drd.bootstrap.AccessHistory;
import com.devexperts.drd.bootstrap.DRDRegistry;
import com.devexperts.drd.bootstrap.RaceHistory;
import com.devexperts.drd.bootstrap.ThreadRaceInfo;

public class RaceHistoryImpl implements RaceHistory {
    private final RacesReportingHistory racesReportingHistory;
    private final AccessHistory accessHistory;

    public RaceHistoryImpl(AccessHistory accessHistory, DRDRegistry registry) {
        this.accessHistory = accessHistory;
        this.racesReportingHistory = new RacesReportingHistory(registry);
    }

    public ThreadRaceInfo getFieldAccess(int ownerId, int nameId, long tid) {
        return accessHistory.getFieldAccess(ownerId, nameId, tid);
    }

    public ThreadRaceInfo getObjectAccess(Object o, int ownerId, long tid) {
        return accessHistory.getObjectAccess(o, ownerId, tid);
    }

    public void trackForeignCall(Object o, int ownerId, int location) {
        accessHistory.saveObjectAccess(o, ownerId, Thread.currentThread().getId(),
                new ThreadRaceInfo(location, new Exception()));
    }

    public boolean shouldReportRace(int location, int raceLocation) {
        return racesReportingHistory.shouldReportRace(location, raceLocation);
    }


    public void trackFieldAccesses(int ownerId, int nameId, int location) {
        accessHistory.saveFieldAccess(ownerId, nameId, Thread.currentThread().getId(),
                new ThreadRaceInfo(location, new Exception()));
    }
}
