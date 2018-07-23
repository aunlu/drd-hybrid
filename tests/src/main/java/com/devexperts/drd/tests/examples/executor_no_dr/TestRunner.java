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

package com.devexperts.drd.tests.examples.executor_no_dr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Should pass even without executor contract because we track all thread forks
 */
public class TestRunner {
    static List<String> o = new ArrayList<String>();
    static Runnable runnable = new Runnable() {
        public void run() {
            o.add("2");
            o = new ArrayList<String>(o);
            System.out.println(o);
            System.out.println("Done!");
        }
    };
    static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        System.out.println("Launching executor_no_dr test ...");
        o.add("1");
        o = new ArrayList<String>(o);
        executor.execute(runnable);
        executor.shutdown();
    }
}
