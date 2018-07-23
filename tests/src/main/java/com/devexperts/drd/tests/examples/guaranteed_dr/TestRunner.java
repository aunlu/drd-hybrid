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

package com.devexperts.drd.tests.examples.guaranteed_dr;

import java.lang.InterruptedException;
import java.lang.Object;
import java.lang.Runnable;
import java.lang.Thread;

public class TestRunner {
    private Thread thread1;
    private Thread thread2;
    //private final Object lock = new Object();
    private Object o;
    private Object o2;

    public static void main(String[] args) {
        System.out.println("Launching guaranteed_dr test ...");
        new TestRunner().execute();
    }

    private void execute() {
        thread1 = new Thread(new TestRunner.RunnableImpl());
        thread2 = new Thread(new TestRunner.RunnableImpl());
        thread1.start();
        thread2.start();
    }

    private void f(Object o) {
        if (o != null) {
            o.toString();
        }
    }

    private final class RunnableImpl implements Runnable {
        public void run() {
            Object o1;
            Object o3;
            for (int i = 0; i < 10; i++) {
                //synchronized (lock) {
                o1 = o;
                o3 = o2;
                //}
                //synchronized (lock) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    //skip
                }
                o = new Object();
                o2 = new Object();
                // }
                f(o1);
                f(o3);
            }
        }
    }
}
