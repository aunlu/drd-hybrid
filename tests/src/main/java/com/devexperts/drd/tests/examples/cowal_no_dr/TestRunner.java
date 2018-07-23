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

package com.devexperts.drd.tests.examples.cowal_no_dr;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestRunner {
    private static final List<A> list = new CopyOnWriteArrayList<A> ();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Launching cowal_no_dr test ...");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 20; i++) {
                    list.add(new A(i));
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                int accumulator = 0;
                for (int i = 0; i < 20; i++) {
                    if (list.size() > 0) {
                        accumulator += list.remove(0).i;
                    }
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(accumulator);
            }
        });
        t1.start();
        t2.start();
        Thread.sleep(100L);
        System.out.println(list.size());
        int accumulator = 0;
        for (A a : list) {
            accumulator += a.i;
        }
        System.out.println(accumulator);
    }

    private static class A {
        int i;

        private A(int i) {
            this.i = i;
        }
    }
}
