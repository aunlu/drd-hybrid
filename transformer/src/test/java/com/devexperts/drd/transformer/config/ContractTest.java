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
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ContractTest {
    @Test
    public void testContractMatching() {
        List<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new Contract("java.util.Map", "keySet,values,entrySet", "", DRDConfig.RaceDetectionMode.ALL));
        contracts.add(new Contract("java.util.List", "listIterator", "", DRDConfig.RaceDetectionMode.ALL));
        contracts.add(new Contract("*", "get*,toString,hashCode,equals,is*,contains*,iter*,has*,size", "", DRDConfig.RaceDetectionMode.ALL));
        ContractsConfig cfg = new ContractsConfig(contracts);
        checkMatch(cfg, "java/util/List", "get", DRDConfig.MethodCallType.READ);
        checkMatch(cfg, "java/util/List", "size", DRDConfig.MethodCallType.READ);
        checkMatch(cfg, "java/util/List", "isEmpty", DRDConfig.MethodCallType.READ);
        checkMatch(cfg, "java/util/List2", "isEmpty", DRDConfig.MethodCallType.READ);
        checkMatch(cfg, "java/util/List2", "set", DRDConfig.MethodCallType.WRITE);
    }

    private void checkMatch(ContractsConfig cfg, String owner, String name, DRDConfig.MethodCallType expected) {
        Assert.assertEquals(expected, cfg.getInfo(owner, name).callType);
    }
}
