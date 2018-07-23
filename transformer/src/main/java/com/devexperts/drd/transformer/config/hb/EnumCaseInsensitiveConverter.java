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

import com.thoughtworks.xstream.converters.enums.EnumSingleValueConverter;

public class EnumCaseInsensitiveConverter extends EnumSingleValueConverter {
    private final Class<? extends Enum> enumType;

    public EnumCaseInsensitiveConverter(Class<? extends Enum> type) {
        super(type);
        this.enumType = type;
    }

    @Override
    public Object fromString(String str) {
        Enum[] constants = enumType.getEnumConstants();
        for (Enum e : constants) {
            if (e.name().equalsIgnoreCase(str)) {
                return e;
            }
        }
        return null;
    }
}
