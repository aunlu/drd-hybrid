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

import com.devexperts.drd.race.impl.StackTrace;

import java.util.Map;

/**
 * Encapsulate access from certain thread to race target (read/write of shared field or execution of shared object's method)
 */
@SuppressWarnings("unused")
public interface Access {
    String FIELD_OWNER = "owner";
    String FIELD_NAME = "name";
    String OBJECT_TYPE = "type";
    String OBJECT_METHOD = "method";

    /**
     * @return line of code where access took place (... at <tt>com.myapp.MyClass.method()</tt> at line:<tt>123</tt>)
     */
    CodeLine getCodeLine();

    /**
     * @return type of access: READ or WRITE
     */
    AccessType getAccessType();

    /**
     *
     * Contains information on target of race for this access.
     * <ul>
     * <li>If race target is <b>FIELD</b> this map would have two keys:
     * <ul>
     *     <li>"owner" ({@link Access#FIELD_OWNER}) - name of class-owner of racy field, e.g. <code>com.myapp.model.User</code></li>
     *     <li>"name" ({@link Access#FIELD_NAME}) - name of racy field itself, e.g. <code>firstName</code></li>
     * </ul>
     * </li>
     * <li>else if race target is <b>OBJECT</b> this map would have two keys: "type", "method" (e.g. "java/lang/Object" and "hashcode")
     * <ul>
     *     <li>"type" ({@link Access#OBJECT_TYPE}) - type of racy object, e.g. <code>java.util.ArrayList</code></li>
     *     <li>"method" ({@link Access#OBJECT_METHOD}) - name of object's method, e.g. <code>add</code></li>
     * </ul>
     * </li>
     * </ul>
     * @see RaceTargetType
     *
     * @return target description in map format
     */
    Map<String, String> getTargetInfo();

    /**
     * @return accessing thread's name
     */
    String getThreadName();

    /**
     * @return accessing thread's tid
     */
    long getTid();

    /**
     * @return accessing thread's stack trace (not always available)
     */
    StackTrace getStackTrace();

    /**
     * @return accessing thread's vector clock (not always available)
     */
    ThreadClock getThreadClock();

    /**
     * @return target vector clock
     */
    DataClock getTargetClock();
}
