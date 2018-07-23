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

public class DelegateTransformer implements MethodTransformer {
    private MethodTransformer delegate;

    public DelegateTransformer(MethodTransformer delegate) {
        this.delegate = delegate;
    }

    public void processEnterSynchronizedMethod() {
        delegate.processEnterSynchronizedMethod();
    }

    public void skipEnterSynchronizedMethod() {
        delegate.skipEnterSynchronizedMethod();
    }

    public void processExitSynchronizedMethod() {
        delegate.processExitSynchronizedMethod();
    }

    public void skipExitSynchronizedMethod() {
        delegate.processExitSynchronizedMethod();
    }

    public void processWait(String desc, Runnable call) {
        delegate.processWait(desc, call);
    }

    public void skipWait() {
        delegate.skipWait();
    }

    public void processJoin(Runnable call) {
        delegate.processJoin(call);
    }

    public void skipJoin() {
        delegate.skipJoin();
    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {
        delegate.processPossibleVertex(potentialVertices, opcode, owner, name, desc, callIfHB, callIfNotHB);
    }

    public void skipPossibleVertex() {
        delegate.skipPossibleVertex();
    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {
        delegate.processForeignCall(opcode, owner, name, desc, line, call);
    }

    public void skipForeignCall() {
        delegate.skipForeignCall();
    }

    public void processVolatile(int opcode, String owner, String name, String desc) {
        delegate.processVolatile(opcode, owner, name, desc);
    }

    public void skipVolatile() {
        delegate.skipVolatile();
    }

    public void processMonitorEnter() {
        delegate.processMonitorEnter();
    }

    public void skipMonitorEnter() {
        delegate.skipMonitorEnter();
    }

    public void processMonitorExit() {
        delegate.processMonitorExit();
    }

    public void skipMonitorExit() {
        delegate.skipMonitorExit();
    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        delegate.processFieldAccess(opcode, owner, name, desc, line);
    }

    public void skipFieldAccess() {
        delegate.skipFieldAccess();
    }
}
