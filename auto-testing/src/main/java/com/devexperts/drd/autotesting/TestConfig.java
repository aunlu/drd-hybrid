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

package com.devexperts.drd.autotesting;

import com.thoughtworks.xstream.annotations.*;

import java.util.*;


@XStreamAlias("Test")
public class TestConfig {
    @XStreamAlias("DRD")
    public DrdConfigs configs = new DrdConfigs();
    @XStreamAlias("Races")
    public Races races = new Races();

    @XStreamOmitField
    Map<String, String> configNamesMap;
    @XStreamOmitField
    Map<String, Set<String>> configRacesMap;
    @XStreamOmitField
    Map<String, Set<String>> raceConfigsMap;

    @XStreamAlias("DRD")
    static class DrdConfigs {
        @XStreamImplicit
        public List<DrdConfig> cList = new ArrayList<>();
    }

    @XStreamAlias("Races")
    static class Races {
        @XStreamImplicit
        public List<Race> rList = new ArrayList<>();
    }

    @XStreamAlias("Config")
    static class DrdConfig {
        @XStreamAlias("name")
        @XStreamAsAttribute
        String cName;
        @XStreamAlias("path")
        @XStreamAsAttribute
        String cPath;
    }

    @XStreamAlias("Race")
    static class Race {
        @XStreamAlias("class")
        @XStreamAsAttribute
        String rClass;
        @XStreamAlias("field")
        @XStreamAsAttribute
        String rField;
        @XStreamAlias("config")
        @XStreamAsAttribute
        String rConfig;
    }
}
