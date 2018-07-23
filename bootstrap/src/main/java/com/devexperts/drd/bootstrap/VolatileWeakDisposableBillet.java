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

public class VolatileWeakDisposableBillet implements AbstractVolatileWeakDisposable {
    private int ownerId;
    private int nameId;
    private Object referent;
    private int hashcode;

    public void set(Object referent, int ownerId, int nameId) {
        this.ownerId = ownerId;
        this.nameId = nameId;
        this.referent = referent;
        hashcode = 31 * (31 * ownerId + nameId) + System.identityHashCode(referent);
    }

    public boolean canDelete() {
        throw new UnsupportedOperationException();
    }

    public int getNameId() {
        return nameId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void clear() {
        referent = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        AbstractVolatileWeakDisposable that = (AbstractVolatileWeakDisposable) o;

        if (hashcode != that.hashCode()) return false;
        if (ownerId != that.getOwnerId()) return false;
        if (nameId != that.getNameId()) return false;
        if (referent != that.get()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    public Object get() {
        return referent;
    }
}
