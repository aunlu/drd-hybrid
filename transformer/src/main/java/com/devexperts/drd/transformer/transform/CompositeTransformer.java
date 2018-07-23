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

import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

/**
 * Transformer for debugging purposes
 */
public class CompositeTransformer implements MethodTransformer {
    private final MethodTransformer main;
    private final MethodTransformer aux;

    public CompositeTransformer(GeneratorAdapter mv, int access, String owner, String name) {
        main = new ProcessingMethodTransformer(mv, access, owner, name, new TrackingMethodTransformer(mv, owner, name));
        aux = new IdentityMethodTransformer(mv, new EmptyTransformer());
    }

    public void processEnterSynchronizedMethod() {
        main.processEnterSynchronizedMethod();
    }

    public void skipEnterSynchronizedMethod() {
        main.skipEnterSynchronizedMethod();
    }

    public void processExitSynchronizedMethod() {
        main.processExitSynchronizedMethod();
    }

    public void skipExitSynchronizedMethod() {
        main.skipExitSynchronizedMethod();
    }

    public void processWait(String desc, Runnable call) {
        main.processWait(desc, call);
    }

    public void skipWait() {
        main.skipWait();
    }

    public void processJoin(Runnable call) {
        main.processJoin(call);
    }

    public void skipJoin() {
        main.skipJoin();
    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {
        main.processPossibleVertex(potentialVertices, opcode, owner, name, desc, callIfHB, callIfNotHB);
    }

    public void skipPossibleVertex() {
        main.skipPossibleVertex();
    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {
        main.processForeignCall(opcode, owner, name, desc, line, call);
    }

    public void skipForeignCall() {
        main.skipForeignCall();
    }

    public void processVolatile(int opcode, String owner, String name, String desc) {
        main.processVolatile(opcode, owner, name, desc);
    }

    public void skipVolatile() {
        main.skipVolatile();
    }

    public void processMonitorEnter() {
        main.processMonitorEnter();
    }

    public void skipMonitorEnter() {
        main.skipMonitorEnter();
    }

    public void processMonitorExit() {
        main.processMonitorExit();
    }

    public void skipMonitorExit() {
        main.skipMonitorExit();
    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        main.processFieldAccess(opcode, owner, name, desc, line);
    }

    public void skipFieldAccess() {
        main.skipFieldAccess();
    }
}
