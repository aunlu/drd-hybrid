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

public class DRDConfigImpl implements DRDConfig {
    private TraceConfig traceConfig;
    private ContractsConfig contractsConfig;

    DRDConfigImpl(ContractsConfig contractsConfig, TraceConfig traceConfig) {
        this.contractsConfig = contractsConfig;
        this.traceConfig = traceConfig;
    }

    public void addToExcludeList(Class[] classes) {
        InstrumentationScopeConfig.getInstance().addToExcludeList(classes);
    }

    public ContractInfo getMethodContractInfo(String owner, String name) {
        return contractsConfig.getInfo(owner, name);
    }

    public boolean shouldPrintAccess(String owner, String field) {
        return traceConfig.shouldPrintTrace(owner, field);
    }

    public boolean shouldTrackFieldAccess(String owner, String field, String caller) {
        return traceConfig.shouldTrackThreadAccesses(owner, field, caller);
    }

    public boolean shouldTrackForeignCall(String owner, String name, String caller) {
        return traceConfig.shouldTrackThreadAccesses(owner, name, caller);
    }

    public boolean shouldPrintSyncOperation(String owner, String method) {
        return traceConfig.shouldPrintSyncOperation(owner, method);
    }

    public boolean shouldPrintDataOperation(String owner, String name, String caller) {
        return traceConfig.shouldPrintClocks(owner, name, caller);
    }
}
