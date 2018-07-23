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

import com.devexperts.drd.bootstrap.DRDLogger;

/**
 * Utility class that checks if stack trace of any thread is longer than specified threshold, that is treated as
 * {@link java.lang.StackOverflowError}. In this case preventive {@link java.lang.RuntimeException} is thrown.
 */
public class StackOverflowDetector {
    private static final int THRESHOLD = 300;
    private static final long SLEEP_PERIOD = 1000L;
    private static int i = 0;

    public static void launch() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                DRDLogger.log("Stack overflow detector launched.");
                while (true) {
                    checkStackOverflow();
                    try {
                        Thread.sleep(SLEEP_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void checkStackOverflow() {
        for (StackTraceElement[] ste : Thread.getAllStackTraces().values()) {
            if (ste.length > THRESHOLD) {
                System.err.println("overflow!!");
                String s = ThreadUtils.toString(ste);
                System.err.println(s);
                DRDLogger.error("StackOverflow prevented \n\n" + ThreadUtils.toString(ste));
                //throw new RuntimeException("StackOverflow prevented.");
            }
        }
    }
}
