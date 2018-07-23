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

package com.devexperts.drd.agent.core;

import com.devexperts.drd.agent.high_scale_lib.NonBlockingHashMap;
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.IHBManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class HBDynamicHelper {
    private final ConcurrentMap<Object, Boolean> cache = new NonBlockingHashMap<Object, Boolean>();
    private final ThreadLocal<ReusableKey> reusableKeys = new ThreadLocal<ReusableKey>() {
        @Override
        protected ReusableKey initialValue() {
            return new ReusableKey();
        }
    };
    private final IHBManager hbManager;

    public HBDynamicHelper(IHBManager hbManager) {
        this.hbManager = hbManager;
    }

    public boolean matches(Class c, String methodName, int vertexId) {
        ReusableKey reusableKey = reusableKeys.get();
        reusableKey.setFields(c, vertexId);
        Boolean result = cache.get(reusableKey);
        if (result != null) {
            return result;
        }
        Guard.INSTANCE.lockHard();
        try {
            final Key key = new Key(c, vertexId);
            final boolean value = calculateMatching(c, methodName, vertexId);
            cache.put(key, value);
            return value;
        } finally {
            Guard.INSTANCE.unlockHard();
        }
    }

    private boolean calculateMatching(Class c, String methodName, int vertexId) {
        List<String> internalNames = getAllAncestors(c);
        String owner = hbManager.getVertexOwner(vertexId);
        for (String className : internalNames) {
            if (className.equals(owner)) {
                DRDLogger.log("Match: " + c.getName() + "." + methodName + " <--> vertex" + vertexId);
                return true;
            }
        }
        DRDLogger.debug("No match: " + c.getName() + "." + methodName + " <--> vertex" + vertexId);
        return false;
    }

    /**
     * @param c class name
     * @return list of internal names of all superclasses and interfaces of specified class in inheritance tree
     */
    private static List<String> getAllAncestors(Class c) {
        final List<String> internalNames = new ArrayList<String>();
        processClass(c, internalNames);
        return internalNames;
    }

    private static void processClass(Class c, List<String> accumulate) {
        //TODO class name is interned => do not modify it to avoid garbage producing
        accumulate.add(c.getName().replace(".", "/"));
        for (Class iface : c.getInterfaces()) {
            processClass(iface, accumulate);
        }
        Class superClass = c.getSuperclass();
        if (superClass != null) {
            processClass(superClass, accumulate);
        }
    }

    private static class Key {
        private final Class c;
        private final int vertexId;
        private final int hashcode;

        private Key(Class c, int vertexId) {
            this.c = c;
            this.vertexId = vertexId;
            hashcode = c.hashCode() * 31 + vertexId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof Key) {
                Key that = (Key) o;
                if (that.hashcode != hashcode) return false;
                return that.vertexId == vertexId && c.getName().equals(that.c.getName());
            } else if (o instanceof ReusableKey) {
                ReusableKey that = (ReusableKey) o;
                if (that.hashcode != hashcode) return false;
                return that.vertexId == vertexId && c.getName().equals(that.c.getName());
            } else return false;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    private static class ReusableKey {
        private Class c;
        private int vertexId;
        private int hashcode;

        public void setFields(Class c, int vertexId) {
            this.c = c;
            this.vertexId = vertexId;
            hashcode = c.hashCode() * 31 + vertexId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof Key) {
                Key that = (Key) o;
                if (that.hashcode != hashcode) return false;
                return that.vertexId == vertexId && c == that.c;
            } else if (o instanceof ReusableKey) {
                ReusableKey that = (ReusableKey) o;
                if (that.hashcode != hashcode) return false;
                return that.vertexId == vertexId && c == that.c;
            } else return false;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }
}
