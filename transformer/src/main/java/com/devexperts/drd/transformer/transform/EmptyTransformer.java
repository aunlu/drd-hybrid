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

package com.devexperts.drd.transformer.transform;

import java.util.List;

public class EmptyTransformer implements MethodTransformer {
    public void processEnterSynchronizedMethod() {

    }

    public void skipEnterSynchronizedMethod() {

    }

    public void processExitSynchronizedMethod() {

    }

    public void skipExitSynchronizedMethod() {

    }

    public void processWait(String desc, Runnable call) {

    }

    public void skipWait() {

    }

    public void processJoin(Runnable call) {

    }

    public void skipJoin() {

    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {

    }

    public void skipPossibleVertex() {

    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {

    }

    public void skipForeignCall() {

    }

    public void processVolatile(int opcode, String owner, String name, String desc) {

    }

    public void skipVolatile() {

    }

    public void processMonitorEnter() {

    }

    public void skipMonitorEnter() {

    }

    public void processMonitorExit() {

    }

    public void skipMonitorExit() {

    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {

    }

    public void skipFieldAccess() {

    }
}
