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

package com.devexperts.drd_test.crash_test;

public class EnumCrasher {
    enum State {OK, ERROR}

    static State[] arr;
    public static void main(String[] args) {
        new Thread(new Runnable() {
            public void run() {
                arr = f();
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                arr = f();
            }
        }).start();
    }

    private static State[] f() {
        return State.values();
    }
}
