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

import com.devexperts.drd.bootstrap.CollectionUtils;
import com.devexperts.drd.bootstrap.IHBManager;

import java.util.*;

public class HBManager implements IHBManager {
    private HBVertex[] verticesById;
    //name -> desc -> vertices
    private final Map<String, Map<String, List<Integer>>> verticesByNameAndDescriptor = new HashMap<String, Map<String, List<Integer>>>();

    private HBManager() {
        final List<HBContract> hbContracts = HBConfigParser.parseConfig();
        verticesById = new HBVertex[HBVertex.idGenerator];
        for (HBContract hbContract : hbContracts) {
            for (HBVertex vertex : hbContract.getVertices()) {
                verticesById[vertex.getId()] = vertex;
                CollectionUtils.putToMapOfMapsOfLists(vertex.getName(), vertex.getDescriptor(), vertex.getId(), verticesByNameAndDescriptor);
            }
        }
    }

    /**
     * Used in dynamic to quickly obtain HBVertex by id
     *
     * @param id id
     * @return vertex
     */
    public HBVertex getHappensBeforeVertex(int id) {
        return verticesById[id];
    }

    public HBVertex[] getVertices() {
        return verticesById;
    }

    /**
     * Called at instrumentation stage
     *
     * @param name       method name
     * @param descriptor method descriptor
     * @return all vertices with specified method name and descriptor
     */
    public List<Integer> getHbVerticesIds(String name, String descriptor) {
        Map<String, List<Integer>> inner = verticesByNameAndDescriptor.get(name);
        return inner == null ? null : inner.get(descriptor);
    }

    public String getVertexOwner(int vertexId) {
        return verticesById[vertexId].getOwner();
    }

    public static HBManager getInstance() {
        return HBManagerSingletonHolder.INSTANCE;
    }

    /**
     * Lazy initialize instance of {@link HBManager}
     */
    private static class HBManagerSingletonHolder {
        public static final HBManager INSTANCE = new HBManager();
    }
}
