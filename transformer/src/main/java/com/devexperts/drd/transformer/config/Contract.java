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

import com.devexperts.drd.bootstrap.DRDConfig;

import java.util.regex.Pattern;

public class Contract {
    public enum ContractType {READ, WRITE, OWNER_MATCH, NO_MATCH}

    private static final String WILDCARD = "*";
    private static final String SEPARATOR = ";|,";

    private final Pattern ownerPattern;
    private Pattern readPattern;
    private Pattern writePattern;
    private boolean checkWriteFirst;
    private final DRDConfig.RaceDetectionMode raceDetectionMode;

    public Contract(String owner, String reads, String writes, DRDConfig.RaceDetectionMode raceDetectionMode) {
        ownerPattern = ConfigUtils.compileFromCfgString(ConfigUtils.toInternalName(owner), true);
        if (reads != null) {
            for (String s : reads.split(SEPARATOR)) {
                if (s.trim().equals(WILDCARD)) {
                    readPattern = ConfigUtils.compileFromCfgString(WILDCARD, false);
                    checkWriteFirst = true;
                    break;
                }
            }
            if (readPattern == null) {
                readPattern = ConfigUtils.compileFromCfgString(reads, false);
            }
        }
        if (writes != null) {
            for (String s : writes.split(SEPARATOR)) {
                if (s.trim().equals(WILDCARD)) {
                    if (checkWriteFirst) throw new IllegalArgumentException("Both reads and writes contain wildcards");
                    writePattern = ConfigUtils.compileFromCfgString(WILDCARD, false);
                    break;
                }
            }
            if (writePattern == null) {
                writePattern = ConfigUtils.compileFromCfgString(writes, false);
            }
        }
        this.raceDetectionMode = raceDetectionMode;
    }

    public ContractType getType(String owner, String name) {
        if (ownerPattern.matcher(owner).matches()) {
            if (checkWriteFirst) {
                if (writePattern != null && writePattern.matcher(name).matches()) return ContractType.WRITE;
                else if (readPattern != null && readPattern.matcher(name).matches()) return ContractType.READ;
            } else {
                if (readPattern != null && readPattern.matcher(name).matches()) return ContractType.READ;
                else if (writePattern != null && writePattern.matcher(name).matches()) return ContractType.WRITE;
            }
            return ContractType.OWNER_MATCH;
        }
        return ContractType.NO_MATCH;
    }

    public DRDConfig.RaceDetectionMode getRaceDetectionMode() {
        return raceDetectionMode;
    }

    @Override
    public String toString() {
        return "Contract{ class: " + ownerPattern + ", read: " + readPattern + ", write: " + writePattern +
                ", raceDetectionMode: " + raceDetectionMode + "}";
    }
}
