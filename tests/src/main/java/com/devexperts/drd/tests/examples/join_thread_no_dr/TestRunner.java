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

package com.devexperts.drd.tests.examples.join_thread_no_dr;

public class TestRunner {
    static Object o;
    static Object o2;

    public static void main(String[] args) {
        System.out.println("Launching join_thread_no_dr test ...");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                o = new Object();
                Object o1 = o;
                o = new Object();
            }
        });
        t1.start();
        try {
            System.out.println("joining thread " + t1.getId());
            t1.join();
            System.out.println("returned from join");
        } catch (InterruptedException e) {
            System.out.println("interrupted");
        }
        System.out.println("code goes");
        o2 = o;
    }
}
