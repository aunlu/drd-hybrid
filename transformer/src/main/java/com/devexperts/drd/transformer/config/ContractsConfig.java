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
import com.devexperts.drd.bootstrap.DRDLogger;

import java.util.ArrayList;
import java.util.List;

public class ContractsConfig {
    private static final DRDConfig.ContractInfo DEFAULT_CONTRACT_INFO =
            new DRDConfig.ContractInfo(DRDConfig.MethodCallType.WRITE, DRDConfig.RaceDetectionMode.ALL);
    private final List<Contract> contracts;

    public ContractsConfig(XDRDConfig.Contracts contracts) {
        this.contracts = new ArrayList<Contract>(contracts.contracts.size());
        for (XDRDConfig.Contract c : contracts.contracts) {
            if (c.raceDetectionMode == null) {
                c.raceDetectionMode = DRDConfig.RaceDetectionMode.ALL;
            }
            this.contracts.add(new Contract(c.owner, c.read, c.write, c.raceDetectionMode));
        }
        DRDLogger.log(toString());
    }

    ContractsConfig(List<Contract> contracts) {
        this.contracts = contracts;
        DRDLogger.log(toString());
    }

    public DRDConfig.ContractInfo getInfo(String owner, String target) {
        DRDConfig.RaceDetectionMode mode = null;
        for (Contract c : contracts) {
            final Contract.ContractType type = c.getType(owner, target);
            switch (type) {
                case READ:
                case WRITE:
                    return new DRDConfig.ContractInfo(convert(type), mode == null ? c.getRaceDetectionMode() : mode);
                case OWNER_MATCH:
                    //todo wtf?? very strange code
                    mode = c.getRaceDetectionMode();
                    break;
                case NO_MATCH:
                    break;
            }
        }
        //default behavior
        return mode == null ? DEFAULT_CONTRACT_INFO :
                new DRDConfig.ContractInfo(DRDConfig.MethodCallType.WRITE, mode);
    }

    private DRDConfig.MethodCallType convert(Contract.ContractType type) {
        switch (type) {
            case READ:
                return DRDConfig.MethodCallType.READ;
            case WRITE:
                return DRDConfig.MethodCallType.WRITE;
            default:
                throw new IllegalStateException("Unexpected type : " + type);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("\nContracts config:\n\n");
        sb.append("Contracts matcher would try to apply following contracts sequentially. If no one matches, target method would be treated as write.\n");
        for (Contract c : contracts) {
            sb.append(c.toString()).append("\n");
        }
        return sb.toString();
    }
}
