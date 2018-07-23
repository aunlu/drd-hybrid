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

import com.devexperts.drd.bootstrap.DRDConfig;
import com.devexperts.drd.transformer.config.hb.EnumCaseInsensitiveConverter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

@XStreamAlias("DRDConfig")
public class XDRDConfig {
    @XStreamAlias("InstrumentationScope")
    InstrumentationScope instrumentationScope;
    @XStreamAlias("TraceTracking")
    List<Target> traceTracking;
    @XStreamAlias("Contracts")
    Contracts contracts;

    @XStreamAlias("InstrumentationScope")
    public static class InstrumentationScope {
        @XStreamAlias("SkipOurFields")
        List<Target> skipOurFields;
        @XStreamAlias("SkipForeignCalls")
        List<Target> skipForeignCalls;
        @XStreamAlias("SyncInterception")
        SyncInterception syncInterception;
        @XStreamAlias("RaceDetection")
        RaceDetection raceDetection;
    }

    @XStreamAlias("SyncInterception")
    public static class SyncInterception {
        @XStreamAlias("defaultPolicy")
        @XStreamAsAttribute
        String defaultPolicy;
        @XStreamImplicit
        List<Rule> rules;
    }

    @XStreamAlias("RaceDetection")
    public static class RaceDetection {
        @XStreamAlias("defaultPolicy")
        @XStreamAsAttribute
        String defaultPolicy;
        @XStreamImplicit
        List<Rule> rules;
    }

    @XStreamAlias("Rule")
    public static class Rule {
        @XStreamAlias("type")
        @XStreamAsAttribute
        String type;
        @XStreamAlias("path")
        @XStreamAsAttribute
        String path;
    }

    @XStreamAlias("Target")
    public static class Target {
        @XStreamAlias("clazz")
        @XStreamAsAttribute
        String owner;
        @XStreamAlias("name")
        @XStreamAsAttribute
        String name;
        @XStreamAlias("type")
        @XStreamAsAttribute
        String type;
        @XStreamAlias("traceDataOperations")
        @XStreamAsAttribute
        boolean traceDataOperations;
        @XStreamAlias("traceSyncOperations")
        @XStreamAsAttribute
        boolean traceSyncOperations;
        @XStreamAlias("storeThreadAccesses")
        @XStreamAsAttribute
        boolean storeThreadAccesses;
        @XStreamAlias("printAccessStackTrace")
        @XStreamAsAttribute
        boolean printAccessStackTrace;
        @XStreamAlias("caller")
        @XStreamAsAttribute
        String caller;
    }

    @XStreamAlias("Contracts")
    public static class Contracts {
        @XStreamImplicit
        List<Contract> contracts;
    }

    @XStreamAlias("Contract")
    public static class Contract {
        @XStreamAsAttribute
        @XStreamAlias("clazz")
        String owner;
        @XStreamAsAttribute
        @XStreamAlias("read")
        String read;
        @XStreamAsAttribute
        @XStreamAlias("write")
        String write;
        @XStreamAsAttribute
        @XStreamAlias("detectRaces")
        @XStreamConverter(EnumCaseInsensitiveConverter.class)
        DRDConfig.RaceDetectionMode raceDetectionMode;
    }
}
