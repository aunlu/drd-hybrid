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

package com.devexperts.drd.tests.examples.synchronized_method_no_dr;

public class TestRunner {
    Object o;
    static Object so;

    public static void main(String[] args) {
        System.out.println("Launching synchronized_method_no_dr test ...");
        final TestRunner tr = new TestRunner();
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(200);
                    tr.f();
                    Thread.sleep(100);
                    g1();
                    try {
                        tr.g();
                    } catch (Exception e) {

                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
        t1.start();
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(150);
                    tr.f();
                    Thread.sleep(100);
                    f1();
                    try {
                        tr.g();
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
        t2.start();
        Thread t3 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                    tr.f2();
                    Thread.sleep(100);
                    g1();
                    try {
                        tr.g2();
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
        t3.start();
        Thread t4 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(50);
                    tr.f2();
                    Thread.sleep(100);
                    f1();
                    try {
                        tr.g2();
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
        t4.start();
        try {
            Thread.sleep(50);
            f1();
            Thread.sleep(100);
            g1();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    public synchronized void f() {
        Object o1 = o;
        o = new Object();
    }

    public synchronized void g() throws Exception {
        Object o1 = o;
        o = new Object();
        throw new Exception();
    }

    public void f2() {
        synchronized (this) {
            Object o1 = o;
            o = new Object();
        }
    }

    public synchronized void g2() throws Exception {
        synchronized (this) {
            Object o1 = o;
            o = new Object();
            throw new Exception();
        }
    }

    public static synchronized void f1() {
        Object o1 = so;
        so = new Object();
    }

    public static void g1() {
        synchronized (TestRunner.class) {
            Object o1 = so;
            so = new Object();
        }
    }
}
