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

public class Location {
    public final int targetOwnerId;
    public final int callerOwnerId;
    public final int targetNameId;
    public final int callerNameId;
    public final int line;

    public Location(int targetOwnerId, int targetNameId, int callerOwnerId, int callerNameId, int line) {
        this.targetOwnerId = targetOwnerId;
        this.targetNameId = targetNameId;
        this.callerOwnerId = callerOwnerId;
        this.callerNameId = callerNameId;
        this.line = line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (callerNameId != location.callerNameId) return false;
        if (callerOwnerId != location.callerOwnerId) return false;
        if (line != location.line) return false;
        if (targetNameId != location.targetNameId) return false;
        if (targetOwnerId != location.targetOwnerId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetOwnerId;
        result = 31 * result + callerOwnerId;
        result = 31 * result + targetNameId;
        result = 31 * result + callerNameId;
        result = 31 * result + line;
        return result;
    }
}
