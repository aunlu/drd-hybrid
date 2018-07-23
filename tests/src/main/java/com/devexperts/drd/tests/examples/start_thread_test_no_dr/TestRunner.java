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

package com.devexperts.drd.tests.examples.start_thread_test_no_dr;

public class TestRunner {
    private Object o = new Object();
    private Thread thread1;
    private Thread thread2;
    private final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("Launching start_thread_test_no_dr test ...");
        new TestRunner().execute();
    }

    private void execute() {
        thread1 = new Thread(new Runnable() {
            public void run() {
                synchronized (lock) {
                    o = new Object();
                }
                final Object o1 = o;
                Object tmp = new Object();
                o = new Object();
                o = tmp;
                tmp = o;
                thread2 = new Thread(new Runnable() {
                    public void run() {
                        Object tmp = new Object();
                        o = new Object();
                        o = tmp;
                        Object o2 = o1;
                        o = o2;
                        o2 = o1;
                        o = o2;
                        Object o3 = o;
                    }
                });
                thread2.start();
            }
        });
        thread1.start();
    }
}
