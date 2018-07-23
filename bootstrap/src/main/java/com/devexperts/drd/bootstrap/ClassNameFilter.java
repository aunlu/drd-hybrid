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

public class ClassNameFilter {
    private final String prefix;
    private Filter<String> filter;

    public ClassNameFilter(String prefix, Filter<String> filter) {
        this.prefix = prefix;
        this.filter = filter;
    }

    public boolean acceptSrcClassName(String name) {
        return filter.accept(name.substring(name.lastIndexOf("[") + 1));
    }

    public boolean acceptModifiedClassName(String name) {
        return filter.accept(getSourceClassName(name));
    }

    public String getSourceClassName(String modifiedClassName) {
        int index = modifiedClassName.lastIndexOf("[") + 1;
        return modifiedClassName.substring(0, index) +
                modifiedClassName.substring(index + prefix.length());
    }

    public String getModifiedClassName(String sourceClassName) {
        int index = sourceClassName.lastIndexOf("[") + 1;
        return sourceClassName.substring(0, index) + prefix +
                sourceClassName.substring(index);
    }
}
