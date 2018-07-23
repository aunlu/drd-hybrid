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

package com.devexperts.drd.transformer.config;

import java.util.Collection;
import java.util.regex.Pattern;

public class ConfigUtils {
    private ConfigUtils() {
    }

    public static boolean containsByPrefix(Collection<String> prefixes, String s) {
        for (String prefix : prefixes) {
            if (s.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * @param s comma- or semicolon- separated list of patterns. From all special symbols only wildcards (*) are supported
     * @return pattern that matches specified list of string patterns or null, if string was null or empty
     */
    public static Pattern compileFromCfgString(String s, boolean patternsArePrefixes) {
        if (s == null || s.trim().length() == 0) return null;
        if (!patternsArePrefixes) {
            s = s.replace("*", ".*").replace(",", "|").replace(";", "|");
        } else {
            s = s.replace("*", ".*").replace(",", ".*|").replace(";", ".*|");
            if (!s.endsWith("*") && !s.endsWith("|")) {
                s += ".*";
            }
        }
        return Pattern.compile(s);
    }

    /**
     * @param patterns list of patterns. From all special symbols only wildcards (*) are supported
     * @return pattern that matches specified list of string patterns or null, if string was null or empty
     */
    public static Pattern compileFromCfgPatternsCollection(Collection<String> patterns, boolean patternsArePrefixes) {
        if (patterns == null || patterns.size() == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (String s : patterns) {
            sb.append(s).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return compileFromCfgString(sb.toString(), patternsArePrefixes);
    }

    public static String toInternalName(String s) {
        return s == null ? null : s.replace(".", "/");
    }
}
