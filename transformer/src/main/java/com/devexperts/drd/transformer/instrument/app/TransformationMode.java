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

package com.devexperts.drd.transformer.instrument.app;

public enum TransformationMode {
    IGNORE("IGNORE"),
    INTERNAL_IGNORE("IGNORE INTERNAL CLASS"),
    APPLICATION_IGNORE("IGNORE APPLICATION CLASS"),
    THREAD("TRANSFORM THREAD"),
    CLASS_LOADER("TRANSFORM CLASS LOADER"),
    DETECT_SYNC("DETECT SYNC ONLY"),
    DETECT_RACES("DETECT SYNC AND RACES");

    private final String description;

    private TransformationMode(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
