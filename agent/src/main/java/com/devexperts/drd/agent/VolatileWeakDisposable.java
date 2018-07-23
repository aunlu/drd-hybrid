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

package com.devexperts.drd.agent;

import com.devexperts.drd.bootstrap.AbstractVolatileWeakDisposable;

import java.lang.ref.WeakReference;

public class VolatileWeakDisposable extends WeakReference<Object> implements AbstractVolatileWeakDisposable {
    final int ownerId;
    final int nameId;
    final int hashcode;

    public VolatileWeakDisposable(Object referent, int ownerId, int nameId) {
        super(referent);
        this.ownerId = ownerId;
        this.nameId = nameId;
        hashcode = 31 * (31 * ownerId + nameId) + System.identityHashCode(referent);
    }

    public boolean canDelete() {
        return get() == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        AbstractVolatileWeakDisposable that = (AbstractVolatileWeakDisposable) o;

        if (hashcode != that.hashCode()) return false;
        if (ownerId != that.getOwnerId()) return false;
        if (nameId != that.getNameId()) return false;
        if (get() != that.get()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    public int getNameId() {
        return nameId;
    }

    public int getOwnerId() {
        return ownerId;
    }
}
