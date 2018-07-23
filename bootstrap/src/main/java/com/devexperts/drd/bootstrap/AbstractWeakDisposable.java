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

@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractWeakDisposable implements WeakDisposable {
    /*Fields are public because they are accessed by composite keys, located in other package (see CKM and CKG)*/
    public int uniqueSyncKey;
    public int ownerId;
    public int nameId;
    public int hashcode;

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public void setNameId(int nameId) {
        this.nameId = nameId;
    }

    public void setUniqueSyncKey(int uniqueSyncKey) {
        this.uniqueSyncKey = uniqueSyncKey;
    }

    public abstract void addBoolean(boolean b, int index);
    public abstract void addByte(byte b, int index);
    public abstract void addShort(short s, int index);
    public abstract void addChar(char c, int index);
    public abstract void addInt(int i, int index);
    public abstract void addLong(long l, int index);
    public abstract void addFloat(float f, int index);
    public abstract void addDouble(double d, int index);
    public abstract void addObject(Object o, int index);

    public abstract AbstractWeakDisposable newInstance();
    public abstract AbstractWeakDisposable copy();

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        return "AWD key=" + uniqueSyncKey + " @" + hashcode + " for " + ownerId + "." + nameId;
    }
}
