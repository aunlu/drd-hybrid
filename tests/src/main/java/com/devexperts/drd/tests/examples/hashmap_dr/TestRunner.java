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

package com.devexperts.drd.tests.examples.hashmap_dr;

import java.util.HashMap;
import java.util.Map;

public class TestRunner {
    private static final Map<String, String> map = new HashMap<String, String>();

    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    String s = map.get(String.valueOf(i));
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                    }
                    map.put(s, s);
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    String s = map.get(String.valueOf(i));
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                    }
                    map.put(s, s);
                }
            }
        });
        t1.start();
        t2.start();
    }
}
