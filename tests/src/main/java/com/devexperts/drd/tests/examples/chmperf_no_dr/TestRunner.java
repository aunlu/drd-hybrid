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

package com.devexperts.drd.tests.examples.chmperf_no_dr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRunner {
    static final Map<Integer, A> map = new ConcurrentHashMap<Integer, A>();

    // static final Integer[] keys = new Integer[100];

    static final int MAX_KEY = 100;
    static final int NUM_THREADS = 100;
    static final int ITERATIONS = 50000;

    static final AtomicInteger c = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Launching chmperf_no_dr test ...");
        long spent = 0L;
        //for (int j = 0; j < 20; j++) {
            c.set(0);
            long time = System.currentTimeMillis();
            Runnable incrementer = new Runnable() {
                public void run() {
                    int accumulator = 0;
                    for (int i = 0; i < ITERATIONS; i++) {
                        Integer key = i % MAX_KEY;
                        final A a = map.get(key);
                        if (a != null) {
                            accumulator += a.i;
                        } else {
                            accumulator = 7;
                        }
                        map.put(key, new A(accumulator));
                    }
                    c.incrementAndGet();
                }
            };

            Thread[] threads = new Thread[NUM_THREADS];
            for (int i = 0; i < threads.length; i++) {

                Thread thread = new Thread(incrementer);
                thread.start();
                threads[i] = thread;
            }

            for (Thread thread : threads) {
                thread.join();
            }
            if (c.get() != NUM_THREADS) {
                System.out.println("OUCH!!!");
            }
            final long l = System.currentTimeMillis() - time;
            //spent += l;
            System.out.println("Test done in " + l + " ms.");
        //}
        //System.out.println("Avg : " + spent/20 + " ms.");
    }

    private static class A {
        private A(int i) {
            this.i = i;
        }

        int i;
    }
}
