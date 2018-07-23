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

package com.devexperts.drd.race;

/**
 * Race type. If class is included into DRD race detection scope than DRD detects races on its fields.
 * Objects from excluded code ("foreign" objects) are treated as black-box data structures and their methods as read or write accesses on them.
 * Therefore for fields race target is field itself while for foreign objects race target is object.
 */
public enum RaceTargetType {
    /**
     * Stands for race on field of certain class/object. Occurs when two threads access this field without proper synchronization.
     */
    FIELD,
    /**
     * Stands for race on certain object. Occurs when two threads execute methods of same object without proper synchronization.
     */
    OBJECT
}
