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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtils {
    private CollectionUtils() {
    }

    public static <K, V> void put(K key, V value, Map<K, List<V>> map) {
        List<V> values = map.get(key);
        if (values == null) {
            values = new ArrayList<V>();
            map.put(key, values);
        }
        values.add(value);
    }

    public static <K, V> void putToMapOfMaps(K key, K value, V innerValue, Map<K, Map<K, V>> map) {
        Map<K, V> values = map.get(key);
        if (values == null) {
            values = new HashMap<K, V>();
            map.put(key, values);
        }
        values.put(value, innerValue);
    }

    public static <K, V> void putToMapOfMapsOfLists(K key, K value, V innerValue, Map<K, Map<K, List<V>>> map) {
        Map<K, List<V>> values = map.get(key);
        if (values == null) {
            values = new HashMap<K, List<V>>();
            map.put(key, values);
        }
        List<V> list = values.get(value);
        if (list == null) {
            list = new ArrayList<V>();
            values.put(value, list);
        }
        list.add(innerValue);
    }
}
