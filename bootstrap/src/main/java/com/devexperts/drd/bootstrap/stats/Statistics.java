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

package com.devexperts.drd.bootstrap.stats;

public interface Statistics {
    void increment(Counters counter, Processing processing, String description);
    void countFieldAccess(int ordinal);
    String dumpAndClear();
    void trackFieldAccess(String description, boolean write);
    String dumpFieldAccesses();
    void trackForeignCall(String description, boolean write);
    void lockedSoft(String name);
    void lockedHard(String name);
    void transformed();
}
