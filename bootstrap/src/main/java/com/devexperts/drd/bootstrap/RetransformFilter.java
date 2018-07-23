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

class RetransformFilter implements Filter<String> {
    static final Filter<String> INSTANCE = new RetransformFilter();

    final String[] include;
    final String[] exclude;

    RetransformFilter() {
        String include = DRDProperties.getProperty("drd.retransform.include");
        String exclude = DRDProperties.getProperty("drd.retransform.exclude");
        this.include = include == null || include.length() == 0 ? new String[0] : include.split(",");
        this.exclude = exclude == null || exclude.length() == 0 ? new String[0] : exclude.split(",");
    }

    public boolean accept(String s) {
        if (s.equals("java.lang.Thread")) return true;
        if (s.startsWith("java.util.concurrent")) return true;
        //for debugging start-up JVM crashes
        for (String ex : exclude) {
            //explicitly excluded => exclude
            if (s.startsWith(ex)) {
                return false;
            }
        }
        if (include.length == 1 && include[0].equals("*")) {
            return true;
        }
        //include only items from it and exclude the others
        for (String in : include) {
            if (s.startsWith(in)) {
                return true;
            }
        }
        return false;
    }
}
