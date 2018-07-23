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

public interface DRDConfig {
    public enum MethodCallType {READ, WRITE}
    public enum RaceDetectionMode {ALL, WRITE_WRITE_ONLY}

    void addToExcludeList(Class[] classes);

    public ContractInfo getMethodContractInfo(String owner, String name);

    public boolean shouldPrintAccess(String owner, String field);

    public boolean shouldTrackFieldAccess(String owner, String field, String caller);

    public boolean shouldTrackForeignCall(String owner, String name, String caller);

    public boolean shouldPrintSyncOperation(String owner, String method);

    public boolean shouldPrintDataOperation(String owner, String name, String caller);

    public class ContractInfo {
        public final MethodCallType callType;
        public final RaceDetectionMode raceDetectionMode;

        public ContractInfo(final MethodCallType callType, final RaceDetectionMode raceDetectionMode) {
            this.callType = callType;
            this.raceDetectionMode = raceDetectionMode;
        }
    }
}
