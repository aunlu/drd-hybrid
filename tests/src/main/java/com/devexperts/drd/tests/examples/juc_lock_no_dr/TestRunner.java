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

package com.devexperts.drd.tests.examples.juc_lock_no_dr;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestRunner {
    private static Thread thread1;
    private static Thread thread2;
    private static final ReadWriteLock rwl = new ReentrantReadWriteLock();
    static Object o = new Object();

    public static void main(String[] args) throws InterruptedException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        System.out.println("Launching juc_lock_no_dr test ...");
        thread1 = new Thread(new Runnable() {
            public void run() {
                Object o1;
                for (int i = 0; i < 5; i++) {
                    try {
                        rwl.writeLock().lock();
                        o1 = o;
                        f(o1);
                    } finally {
                        rwl.writeLock().unlock();
                    }
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        rwl.writeLock().lock();
                        f(o);
                        o = new Object();
                        f(o);
                    } finally {
                        rwl.writeLock().unlock();
                    }
                }
            }
        });
        thread2 = new Thread(new Runnable() {
            public void run() {
                Object o1;
                for (int i = 0; i < 5; i++) {
                    try {
                        rwl.writeLock().lock();
                        f(o);
                        o = new Object();
                        f(o);
                    } finally {
                        rwl.writeLock().unlock();
                    }
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        rwl.writeLock().lock();
                        f(o);
                        o1 = o;
                        f(o1);
                    } finally {
                        rwl.writeLock().unlock();
                    }
                }
            }
        });
        thread1.start();
        thread2.start();
    }

    private static String f(Object o) {
        return o.hashCode() + o.toString();
    }
}
