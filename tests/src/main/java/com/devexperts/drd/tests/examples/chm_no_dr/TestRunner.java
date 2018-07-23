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

package com.devexperts.drd.tests.examples.chm_no_dr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestRunner {
    static Map<String, A> map = new ConcurrentHashMap<String, A>();
    private static final String KEY = "abc";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Launching chm_no_dr test ...");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                int accumulator = 0;
                for (int i = 0; i < 20; i++) {
                    final A a = map.get(KEY);
                    if (a != null) {
                        accumulator += a.i;
                        //a.i = 7;
                    } else {
                        accumulator = 7;
                    }
                    map.put(KEY, new A(accumulator));
                    Thread.yield();
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                int accumulator = 0;
                for (int i = 0; i < 20; i++) {
                    final A a = map.get(KEY);
                    if (a != null) {
                        accumulator += a.i;
                        //a.i = 11;
                    } else {
                        accumulator = 11;
                    }
                    map.put(KEY, new A(accumulator));
                    Thread.yield();
                }
            }
        });
        t1.start();
        t2.start();
    }

    private static class A {
        private A(int i) {
            this.i = i;
        }

        int i;
    }
}
