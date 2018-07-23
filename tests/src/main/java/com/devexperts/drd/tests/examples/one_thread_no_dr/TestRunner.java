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

package com.devexperts.drd.tests.examples.one_thread_no_dr;

public class TestRunner {
    static volatile int i;
    Object o;

    public static void main(String[] args) {
        System.out.println("Launching one_thread_no_dr test ...");
        TestRunner tr = new TestRunner();
        for (int i = 0; i < 10; i++) {
            tr.f();
            tr.g();
        }
        System.out.println(i);
    }

    void f() {
        o = new Object();
        Object o1 = o.toString();
        i += System.identityHashCode(o1);
    }

    void g() {
        o = new Object();
        Object o2 = o.toString();
        i += System.identityHashCode(o2);
    }
}
