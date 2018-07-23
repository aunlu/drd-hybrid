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

package com.devexperts.drd.transformer.config.hb;

import java.util.List;

public class HBVertex {
    public static int idGenerator = 0;

    private final String owner;
    private final String name;
    private final String descriptor;
    private final int hbContractId;
    private final int id;
    private final List<Integer> argsIndices;
    private final SynchronizationPointType type;
    private final boolean shouldReturnTrue;

    public HBVertex(int hbContractId, String owner, String name, String descriptor, List<Integer> argsIndices, SynchronizationPointType type, boolean shouldReturnTrue) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.hbContractId = hbContractId;
        this.argsIndices = argsIndices;
        this.type = type;
        this.shouldReturnTrue = shouldReturnTrue;
        this.id = idGenerator++;
    }

    public int getId() {
        return id;
    }

    public int getHbContractId() {
        return hbContractId;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public List<Integer> getArgsIndices() {
        return argsIndices;
    }

    public SynchronizationPointType getType() {
        return type;
    }

    public boolean isShouldReturnTrue() {
        return shouldReturnTrue;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("HBVertex");
        sb.append("{argsIndices=").append(argsIndices);
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", descriptor='").append(descriptor).append('\'');
        sb.append(", hbContractId=").append(hbContractId);
        sb.append(", id=").append(id);
        sb.append(", type=").append(type);
        sb.append(", shouldReturnTrue=").append(shouldReturnTrue);
        sb.append('}');
        return sb.toString();
    }
}
