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

import org.objectweb.asm.Type;

import java.util.List;

/**
 * The class that does actual instrumentation of main instructions in two possible ways:
 * <ul>
 * <li><i>process</i> - skip target instruction via DRD interceptor and invoke instruction itself - either explicitly or via callback.
 * Tracking code might be inserted before and/or after target instruction, but stack state after process*
 * method executes would usually be the same as if pure target instruction was executed.</li>
 * <li><i>skip</i> - store the fact that target instruction is not tracked via DRD interceptor. Does not modify the stack.</li>
 * </ul>
 * Extracting actual instrumentation to standalone class provides ability to mock it for testing purposes.
 */
public interface MethodTransformer {
    /**
     * Executed when entrance to synchronized method should be processed
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void processEnterSynchronizedMethod();

    /**
     * Executed when entrance to synchronized method should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipEnterSynchronizedMethod();

    /**
     * Executed when exit from synchronized method should be processed
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void processExitSynchronizedMethod();

    /**
     * Executed when exit from synchronized method should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipExitSynchronizedMethod();

    /**
     * Executed when {@link Object#wait()} call should be processed
     * <br/><br/>
     * EFFECT (STACK): same as by pure callback execution
     *
     * @param desc method descriptor
     * @param call wrapped callback that actually would execute target method
     */
    void processWait(String desc, Runnable call);

    /**
     * Executed when {@link Object#wait()} call should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipWait();

    /**
     * Executed when {@link Thread#join()} call should be processed.
     * <br/><br/>
     * EFFECT (STACK): same as by pure callback execution
     *
     * @param call wrapped callback that actually would execute target method
     */
    void processJoin(Runnable call);

    /**
     * Executed when {@link Thread#join()} call should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipJoin();

    /**
     * Executed when target method call is possible happens-before vertex and should be processed.
     * <br/><br/>
     * EFFECT (STACK): same as by pure callback execution
     *
     * @param potentialVertices complete list of vertices, that target call possibly match
     * @param opcode            the opcode of the type instruction to be visited. This opcode
     *                          is either INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
     *                          INVOKEINTERFACE.
     * @param owner             the internal name of the method's owner class (see
     *                          {@link Type#getInternalName() getInternalName}).
     * @param name              the method's name.
     * @param desc              the method's descriptor (see {@link Type Type}).
     * @param callIfHB          wrapped callback that actually would execute target method if it is a vertex
     * @param callIfNotHB       wrapped callback that actually would execute target method if it is not a vertex
     */
    void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB);

    /**
     * Executed when target method call is possible happens-before vertex and should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipPossibleVertex();

    /**
     * Executed when foreign call invocation should be processed.
     * <br/><br/>
     * STACK: modified only by callback execution.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode
     *               is either INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
     *               INVOKEINTERFACE.
     * @param owner  the internal name of the method's owner class (see
     *               {@link Type#getInternalName() getInternalName}).
     * @param name   the method's name.
     * @param desc   the method's descriptor (see {@link Type Type}).
     * @param call   wrapped callback that actually would execute target method
     * @param line   instruction line number (for data race report)
     */
    void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call);

    /**
     * Executed when target method call is possible happens-before vertex and should be skipped
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipForeignCall();

    /**
     * Executed when volatile instruction should be processed.
     * <br/><br/>
     * EFFECT (STACK): same as by performing target instruction
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode
     *               is either GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
     * @param owner  the internal name of the field's owner class (see
     *               {@link Type#getInternalName() getInternalName}).
     * @param name   the field's name.
     * @param desc   the field's descriptor (see {@link Type Type}).
     */
    void processVolatile(int opcode, String owner, String name, String desc);

    /**
     * Executed when volatile instruction should be skipped.
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipVolatile();

    /**
     * Executed when monitor enter should be processed.
     * <br/><br/>
     * EFFECT (STACK): expect owner on the stack and removes it, but does not perform actual monitorenter
     */
    void processMonitorEnter();

    /**
     * Executed when monitor enter should be skipped.
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipMonitorEnter();

    /**
     * Executed when monitor exit should be processed.
     * <br/><br/>
     * EFFECT (STACK): same as by performing target instruction (monitorexit)
     */
    void processMonitorExit();

    /**
     * Executed when monitor exit should be skipped.
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipMonitorExit();

    /**
     * Executed when non-volatile field instruction should be processed.
     * <br/><br/>
     * EFFECT (STACK): same as by performing target instruction
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode
     *               is either GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
     * @param owner  the internal name of the field's owner class (see
     *               {@link Type#getInternalName() getInternalName}).
     * @param name   the field's name.
     * @param desc   the field's descriptor (see {@link Type Type}).
     * @param line   instruction line number (for data race report)
     */
    void processFieldAccess(int opcode, String owner, String name, String desc, int line);

    /**
     * Executed when field access should be skipped.
     * <br/><br/>
     * EFFECT (STACK): no
     */
    void skipFieldAccess();
}

