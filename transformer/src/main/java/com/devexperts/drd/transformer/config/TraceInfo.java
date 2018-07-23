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

package com.devexperts.drd.transformer.config;

import java.util.regex.Pattern;

public class TraceInfo {
    public final boolean printDataOperations;
    public final boolean printThreadAccess;
    public final boolean trackThreadAccess;
    public final boolean printSyncOperations;
    public final Pattern caller;

    public TraceInfo(boolean printDataOperations, boolean printThreadAccess, boolean trackThreadAccess, boolean printSyncOperations,
                     Pattern caller) {
        this.printDataOperations = printDataOperations;
        this.printThreadAccess = printThreadAccess;
        this.trackThreadAccess = trackThreadAccess;
        this.printSyncOperations = printSyncOperations;
        this.caller = caller;
    }

    boolean nothing() {
        return !printDataOperations && !printThreadAccess && !trackThreadAccess && !printSyncOperations;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(" ");
        if (printDataOperations) sb.append("printDataOperations, ");
        if (printThreadAccess) sb.append("printThreadAccess, ");
        if (trackThreadAccess) sb.append("trackThreadAccess, ");
        if (printSyncOperations) sb.append("printSyncOperations, ");
        if (sb.length() <= 2) {
            sb.append("nothing");
        } else sb.delete(sb.length() - 2, sb.length());
        sb.append(" for ").append(caller == null ? "any caller" : "caller, matching pattern '" + caller.pattern() + "'");
        return sb.toString();
    }
}
