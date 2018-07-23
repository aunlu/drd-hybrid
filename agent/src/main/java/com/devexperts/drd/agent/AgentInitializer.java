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

package com.devexperts.drd.agent;

import com.devexperts.drd.agent.core.*;
import com.devexperts.drd.bootstrap.*;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class AgentInitializer {
    public static void setup(ITransformation transformation) {
        AccessHistoryImpl accessHistory = new AccessHistoryImpl();
        DRDEntryPoint.setAccessHistory(accessHistory);
        DRDEntryPoint.setRegistry(new DRDRegistryImpl(accessHistory));
        DRDEntryPoint.setStatistics(new InternalStatistics());
        DRDEntryPoint.setDataProvider(new CachingDataProvider(transformation));
        DRDEntryPoint.setInterceptor(createInterceptor(transformation));
        //StackOverflowDetector.launch();
        new ThreadDumper(DRDProperties.getLogDir() + File.separatorChar + "thread-dumps.log", 20000L, 5000L).start();
        registerThreads();
        //init internals
        BlackHole.BLACK_HOLE.print(ClocksStorage.getSynClock(new Object()));
    }

    private static void registerThreads() {
        for (Thread t : ThreadUtils.getAllThreads()) {
            DRDEntryPoint.getRegistry().registerThread(t);
        }
    }

    private static DRDInterceptor createInterceptor(ITransformation transformation) {
        switch (DRDProperties.metrics) {
            case FAKE_GUARDED_INTERCEPTOR:
                return new GuardedInterceptor(new MockInterceptor());
            //case FAKE_INTERCEPTOR:
            case FLAG_IGNORE:
                return new MockInterceptor();
            default:
                return new GuardedInterceptor(new VerboseVectorClockInterceptor());
        }
    }
}
