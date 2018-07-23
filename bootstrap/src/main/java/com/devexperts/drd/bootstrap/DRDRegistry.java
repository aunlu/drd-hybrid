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

import java.util.List;

public interface DRDRegistry {
    void registerThread(Thread thread);

    public String getThreadName(long tid);

    int registerClassName(String className);

    String getClassName(int id);

    int registerFieldOrMethodName(String name);

    String getFieldOrMethodName(int id);

    int registerLocation(int targetOwnerId, int targetNameId, int callerOwnerId, int callerNameId, int line);

    Location getLocation(int id);

    void registerDataClock(int ownerId);

    void unregisterDataClock(int ownerId);

    void registerInstrumentedClass(String className);

    boolean isInstrumented(int ownerId);

    void registerFinalField(int ownerId, int fieldNameId);

    boolean isFinal(int ownerId, int fieldNameId);

    void registerVolatileField(int ownerId, int fieldNameId);

    void registerUsualField(int ownerId, int fieldNameId);

    boolean hasField(int ownerId, int fieldNameId);

    boolean isVolatile(int ownerId, int fieldNameId);

    void registerEnum(int ownerId);

    boolean isEnum(int ownerId);

    RaceHistory getRaceHistory();

    List<DataClockStats> getDataClockHistogram();
}
