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

import com.devexperts.drd.agent.high_scale_lib.NonBlockingSetInt;
import com.devexperts.drd.bootstrap.*;

import java.util.Set;

class RacesReportingHistory {
    //TODO possible races skipping (hashcode is not unique)
    private final Set<Integer> reported = new NonBlockingSetInt();
    private final DRDRegistry registry;

    RacesReportingHistory(DRDRegistry registry) {
        this.registry = registry;
    }

    boolean shouldReportRace(int location1, int location2) {
        Location l1 = registry.getLocation(location1);
        Location l2 = registry.getLocation(location2);
        return reported.add(location1 > location2 ? getHashCode(l1, l2) : getHashCode(l2, l1));
    }

    private static int getHashCode(Location l1, Location l2) {
        switch (DRDProperties.racesGrouping) {
            case CALL_LOCATION:
                return 31 * l1.hashCode() + l2.hashCode();
            case CALL_CLASS_AND_METHOD:
                return 31 * (31 * (31 * l1.targetOwnerId + l1.callerOwnerId) + l1.callerNameId) +
                        (31 * (31 * l2.targetOwnerId + l2.callerOwnerId) + l2.callerNameId);
            case CALL_CLASS:
                return 31 * (31 * l1.targetOwnerId + l1.callerOwnerId) +
                        (31 * l2.targetOwnerId + l2.callerOwnerId);
            default: throw new IllegalArgumentException("Unknown races grouping : " + DRDProperties.racesGrouping);
        }
    }
}
