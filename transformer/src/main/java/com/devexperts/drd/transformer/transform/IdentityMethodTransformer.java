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
 * Transformer that just invokes target instruction (i.e. does no additional transformation)
 */
public class IdentityMethodTransformer extends DelegateTransformer {
    private GeneratorAdapter mv;

    public IdentityMethodTransformer(GeneratorAdapter mv, MethodTransformer parent) {
        super(parent);
        this.mv = mv;
    }

    public void processWait(String desc, Runnable call) {
        call.run();
        super.processWait(desc, call);
    }

    public void processJoin(Runnable call) {
        call.run();
        super.processJoin(call);
    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {
        callIfNotHB.run();
        super.processPossibleVertex(potentialVertices, opcode, owner, name, desc, callIfHB, callIfNotHB);
    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {
        call.run();
        super.processForeignCall(opcode, owner, name, desc, line, call);
    }

    public void processVolatile(int opcode, String owner, String name, String desc) {
        mv.visitFieldInsn(opcode, owner, name, desc);
        super.processVolatile(opcode, owner, name, desc);
    }

    public void processMonitorExit() {
        mv.monitorExit();
        super.processMonitorExit();
    }

    public void processMonitorEnter() {
        mv.pop();
        super.processMonitorEnter();
    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        mv.visitFieldInsn(opcode, owner, name, desc);
        super.processFieldAccess(opcode, owner, name, desc, line);
    }
}
