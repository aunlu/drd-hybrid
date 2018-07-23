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

import java.util.ArrayList;
import java.util.List;

public class HBContract {
    private static int idGenerator = 0;

    public HBContract() {
        this.id = idGenerator++;
    }

    private final int id;
    private final List<HBVertex> vertices = new ArrayList<HBVertex>();

    void addVertex(SynchronizationPointType type, String owner, String name, String descriptor, List<Integer> argIndices, boolean shouldReturnTrue) {
        vertices.add(new HBVertex(id, owner, name, descriptor, argIndices, type, shouldReturnTrue));
    }

    public List<HBVertex> getVertices() {
        return vertices;
    }

    @Override
    public String toString() {
        return "HBContract" + "{id=" + id + ", vertices=" + vertices + '}';
    }
}
