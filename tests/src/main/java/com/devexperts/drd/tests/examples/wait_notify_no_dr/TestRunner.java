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

package com.devexperts.drd.tests.examples.wait_notify_no_dr;

public class TestRunner {
    static final Object lock = new Object();
    static Object o = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Launching wait_notify_no_dr test ...");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    //ignore
                }
                synchronized (lock) {
                    o = new Object();
                    Object o1 = o;
                    lock.notify();
                    System.out.println("notify");
                }
            }
        });
        t1.start();
        synchronized (lock) {
            lock.wait(2000L);
            System.out.println("returned from wait");
        }
        Object o2 = o;
        o = new Object();
    }
}
