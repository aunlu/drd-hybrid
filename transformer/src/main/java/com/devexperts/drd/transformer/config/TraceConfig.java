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

import com.devexperts.drd.bootstrap.CollectionUtils;
import com.devexperts.drd.bootstrap.DRDLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TraceConfig {
    public static final String WILDCARD = "*";

    //TODO we need patterns here as keys.
    private final Map<String, Map<String, TraceInfo>> traceInfos; //owner -> field -> info
    private final Map<String, TraceInfo> anyFieldTraceInfos; //owner -> info

    TraceConfig(List<XDRDConfig.Target> accesses) {
        traceInfos = new HashMap<String, Map<String, TraceInfo>>();
        anyFieldTraceInfos = new HashMap<String, TraceInfo>();
        for (XDRDConfig.Target target : accesses) {
            String owner = ConfigUtils.toInternalName(target.owner);
            if (owner.endsWith("/")) owner = owner.substring(0, owner.length() - 1);
            String[] fields = target.name.split(",|;");
            for (String field : fields) {
                TraceInfo traceInfo = new TraceInfo(target.traceDataOperations, target.printAccessStackTrace,
                        target.storeThreadAccesses, target.traceSyncOperations,
                        ConfigUtils.compileFromCfgString(ConfigUtils.toInternalName(target.caller), true));
                if (!traceInfo.nothing()) {
                    if (field.equals(WILDCARD) || field.length() == 0) {
                        anyFieldTraceInfos.put(owner, traceInfo);
                    } else {
                        if (!anyFieldTraceInfos.containsKey(owner)) {
                            CollectionUtils.putToMapOfMaps(owner, field, traceInfo, traceInfos);
                        }
                    }
                } else DRDLogger.log("Nothing for " + owner + "." + field);
            }
        }
        DRDLogger.log("Trace config: " + toString());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n").append(traceInfos.size() > 0 ? traceInfos : "NONE").append("\n");
        sb.append("*: ").append(anyFieldTraceInfos).append("\n");
        return sb.toString();
    }

    public boolean shouldPrintClocks(String owner, String name, String caller) {
        TraceInfo ti = getTraceInfo(owner, name);
        return ti != null && ti.printDataOperations && (ti.caller == null || ti.caller.matcher(caller).matches());
    }

    public boolean shouldTrackThreadAccesses(String owner, String name, String caller) {
        TraceInfo ti = getTraceInfo(owner, name);
        return ti != null && ti.trackThreadAccess && (ti.caller == null || ti.caller.matcher(caller).matches());
    }

    public boolean shouldPrintTrace(String owner, String name) {
        TraceInfo ti = getTraceInfo(owner, name);
        return ti != null && ti.printThreadAccess;
    }

    public boolean shouldPrintSyncOperation(String owner, String method) {
        TraceInfo ti = getTraceInfo(owner, method);
        return ti != null && ti.printSyncOperations;
    }

    private TraceInfo getTraceInfo(String owner, String field) {
        Map<String, TraceInfo> infos = traceInfos.get(owner);
        if (infos != null) {
            TraceInfo ti = infos.get(field);
            if (ti != null) return ti;
        }
        return anyFieldTraceInfos.get(owner);
    }
}
