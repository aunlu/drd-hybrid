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

package com.devexperts.drd.tests.examples.clq_no_dr;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class TestRunner {
    private static final Queue<A> queue = new ArrayBlockingQueue<A>(10);

    public static void main(String[] args) {
        System.out.println("Launching clq_no_dr test ...");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 20; i++) {
                    queue.offer(new A(i));
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int accumulator = 0;
                for (int i = 0; i < 20; i++) {
                    A poll = queue.poll();
                    if (poll != null) {
                        accumulator += poll.i;
                    }
                }
                System.out.println(accumulator);
            }
        });
        t1.start();
        t2.start();
    }

    private static class A {
        int i;

        private A(int i) {
            this.i = i;
        }
    }
}
