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

import com.devexperts.drd.bootstrap.stats.Statistics;

/**
 * DRD entry point, used both by agent and transformer.
 */
@SuppressWarnings({"UnusedDeclaration"})

public class DRDEntryPoint {
    private static volatile DRDInterceptor interceptor;
    private static volatile DRDRegistry registry;
    private static volatile DataProvider dataProvider;
    private static volatile ICompositeKeysManager compositeKeysManager;
    private static volatile AccessHistory accessHistory;
    private static volatile Statistics statistics;

    /**
     * Obtains reference to DRD interceptor. Calls of this method are injected by DRD transformer.
     */
    public static DRDInterceptor getInterceptor() {
        return interceptor;
    }

    public static Statistics getStatistics() {
        return statistics;
    }

    public static void setStatistics(Statistics statistics) {
        DRDEntryPoint.statistics = statistics;
    }

    public static void setInterceptor(DRDInterceptor interceptor) {
        DRDEntryPoint.interceptor = interceptor;
    }

    public static AccessHistory getAccessHistory() {
        return accessHistory;
    }

    public static void setAccessHistory(AccessHistory accessHistory) {
        DRDEntryPoint.accessHistory = accessHistory;
    }

    public static DRDRegistry getRegistry() {
        return registry;
    }

    public static DataProvider getDataProvider() {
        return dataProvider;
    }

    public static void setDataProvider(DataProvider dataProvider) {
        DRDEntryPoint.dataProvider = dataProvider;
    }

    public static void setRegistry(DRDRegistry registry) {
        DRDEntryPoint.registry = registry;
    }

    public static byte[] getCompositeKeyBytes(String className) {
        return DRDEntryPoint.compositeKeysManager.getCompositeKeyBytesByName(className);
    }

    public static void setCompositeKeysManager(ICompositeKeysManager compositeKeysManager) {
        DRDEntryPoint.compositeKeysManager = compositeKeysManager;
    }
}
