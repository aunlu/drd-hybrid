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

package com.devexperts.drd.transformer.config.contract;

import com.devexperts.drd.transformer.config.ConfigUtils;

import java.util.Collection;
import java.util.regex.Pattern;

public class AnyOwnerMatcher implements TargetMatcher {
    private Pattern pattern;

    public AnyOwnerMatcher(Collection<String> owners) {
        pattern = ConfigUtils.compileFromCfgPatternsCollection(owners, false);
    }

    public boolean matches(String owner, String name) {
        return pattern != null && pattern.matcher(name).matches();
    }

    @Override
    public String toString() {
        return pattern == null ? "" : "Any owner if name matches '" + pattern.toString() + "'.";
    }
}
