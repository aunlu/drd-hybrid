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

public class Indexer<T> {
    private static final int MIN_LENGTH = 8;

    private volatile Core<T> core = new Core<T>(MIN_LENGTH);
    private volatile int size;

    public int size() {
        return size;
    }

    /**
     * Does not need external synchronization
     */
    public T get(int id) {
        return core.get(id);
    }

    /**
     * Does not need external synchronization
     */
    public int get(T o) {
        return core.get(o);
    }

    /**
     * Does not need external synchronization
     */
    public int register(T o) {
        int id = core.get(o);
        if (id == 0) {
            return registerImpl(o);
        }
        return id;
    }

    private synchronized int registerImpl(T o) {
        int id = core.get(o);
        if (id == 0) {
            id = ++size;
            core.register(o, id);
            if (size >= core.length / 2) // 50% fill factor for speed
                rehash();
        }
        return id;
    }

    private void rehash() {
        Core<T> oldCore = core;
        Core<T> newCore = new Core<T>(2 * oldCore.length);
        for (int i = 0; i < oldCore.length; i++)
            if (oldCore.objects[i] != null)
                newCore.register((T) oldCore.objects[i], oldCore.ids[i]);
        core = newCore;
    }

    private static class Core<T> {
        final int mask;
        final int length;
        final Object[] objects;
        final int[] ids;
        final Object[] ids2object;

        Core(int length) {
            this.length = length;
            mask = length - 1;
            objects = new Object[length];
            ids = new int[length];
            ids2object = new Object[length];
        }

        /**
         * Does not need external synchronization
         */
        public T get(int id) {
            if (id >= length) {
                return null;
            }
            return (T) ids2object[id];
        }

        /**
         * Does not need external synchronization
         */
        private int get(T o) {
            int i = o.hashCode() & mask;
            Object s;
            while (!o.equals(s = objects[i])) {
                if (s == null) {
                    return 0;
                }

                if (i == 0)
                    i = length;
                i--;
            }
            return ids[i];
        }

        /**
         * Needs external synchronization
         */
        private void register(T o, int id) {
            int i = o.hashCode() & mask;
            Object s;
            while (!o.equals(s = objects[i])) {
                if (s == null) {
                    ids[i] = id;
                    objects[i] = o;
                    ids2object[id] = o;
                    return;
                }

                if (i == 0)
                    i = length;
                i--;
            }
            throw new IllegalStateException();
        }
    }
}
