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

package com.devexperts.drd.gui;

import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.Race;

import java.util.*;

public class ConfigUpdater {
    private final Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> methods = new HashMap<String, Set<String>>();

    public String getConfigChange(List<Race> races) {
        for (Race race : races) {
            Map<String, String> targetInfo = race.getCurrentAccess().getTargetInfo();
            switch (race.getRaceTargetType()) {
                case FIELD:
                    put(targetInfo.get(Access.FIELD_OWNER), targetInfo.get(Access.FIELD_NAME), fields);
                    break;
                case OBJECT:
                    put(targetInfo.get(Access.OBJECT_TYPE), targetInfo.get(Access.OBJECT_METHOD), methods);
                    break;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<!--            PROJECT SPECIFIC FIELDS            -->\n");
        for (Map.Entry<String, Set<String>> e : fields.entrySet()) {
            sb.append(toStringField(e.getKey(), toCSV(e.getValue()))).append("\n");
        }
        sb.append("\n\n<!--            PROJECT SPECIFIC OBJECTS            -->\n");
        for (Map.Entry<String, Set<String>> e : methods.entrySet()) {
            sb.append(toStringMethod(e.getKey(), toCSV(e.getValue()))).append("\n");
        }
        return sb.toString();
    }

    private static String toStringField(String clazz, String field) {
        return "<Target clazz=\"" + clazz + "\" name=\"" + field + "\" storeThreadAccesses=\"true\"/>";
    }

    private static String toStringMethod(String clazz, String caller) {
        return "<Target clazz=\"" + clazz + "\" name=\"*\" caller=\"" + caller + "\" storeThreadAccesses=\"true\"/>";
    }

    private static String toCSV(Collection<String> strings) {
        StringBuilder tmp = new StringBuilder();
        for (String field : strings) {
            tmp.append(field).append(",");
        }
        if (tmp.length() > 0) {
            tmp.deleteCharAt(tmp.length() - 1);
        }
        return tmp.toString();
    }

    private static <K, V> void put(K key, V value, Map<K, Set<V>> map) {
        Set<V> set = map.get(key);
        if (set == null) {
            set = new HashSet<V>();
            map.put(key, set);
        }
        set.add(value);
    }
}
