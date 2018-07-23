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

import com.devexperts.drd.bootstrap.DRDConfig;
import com.devexperts.drd.bootstrap.DRDEntryPoint;
import com.devexperts.drd.bootstrap.DRDProperties;
import com.devexperts.drd.bootstrap.DRDRegistry;
import com.devexperts.drd.transformer.config.DRDConfigManager;
import com.devexperts.drd.transformer.config.hb.HBManager;
import com.devexperts.drd.transformer.config.hb.HBVertex;
import com.devexperts.drd.transformer.config.hb.SynchronizationPointType;
import com.devexperts.drd.transformer.instrument.Constants;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import com.devexperts.drd.transformer.instrument.InterceptorMethod;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ProcessingMethodTransformer extends DelegateTransformer {
    private static final Method GET_CLASS_METHOD = new Method("getClass", Constants.CLASS_TYPE, Constants.EMPTY_TYPE_ARRAY);
    private final String methodOwner;
    private final String methodName;
    private int ownerId;
    private int nameId;
    private final GeneratorAdapter mv;
    private final boolean isStatic;
    private final DRDRegistry registry = DRDEntryPoint.getRegistry();

    public ProcessingMethodTransformer(GeneratorAdapter mv, int access, String owner, String name, MethodTransformer delegate) {
        super(delegate);
        this.mv = mv;
        this.methodOwner = owner;
        this.methodName = name;
        ownerId = registry.registerClassName(owner);
        nameId = registry.registerFieldOrMethodName(name);
        isStatic = (access & ACC_STATIC) != 0;
    }

    public void processEnterSynchronizedMethod() {
        loadThisOrClass();
        monitorEnter();
        super.processEnterSynchronizedMethod();
    }

    public void processExitSynchronizedMethod() {
        loadThisOrClass();
        monitorExit();
        super.processExitSynchronizedMethod();
    }

    public void processWait(String desc, Runnable call) {
        //STACK: ..., <owner>, <args>
        int[] args = InstrumentationUtils.storeArgsToLocals(mv, desc);
        //STACK: <owner>
        beforeWait();
        //STACK: <owner>
        mv.dup();
        //STACK: <owner> <owner>
        InstrumentationUtils.loadArgsFromLocals(mv, args);
        //STACK: <owner> <owner> <args>
        call.run();
        //STACK: <owner>
        afterWait();
        //STACK: <empty>
        super.processWait(desc, call);
    }

    public void processJoin(Runnable call) {
        //We want to know, if it is Thread.join() call or not.
        //STACK: <owner>
        mv.dup();
        mv.instanceOf(Constants.THREAD_TYPE);
        final Label notThread = new Label();
        final Label done = new Label();
        mv.ifZCmp(IFEQ, notThread);
        //STACK: <owner>
        mv.dup();
        call.run();
        //now join() call returned, and we have to load target thread's vc to our vc
        //STACK: <owner>
        mv.checkCast(Constants.THREAD_TYPE);
        //STACK: <thread>
        join();
        //STACK: empty
        mv.goTo(done);

        mv.mark(notThread);
        //STACK: <thread>
        //TODO BUG it may be HB contract + detect races
        call.run();
        //STACK: empty
        mv.mark(done);
        //STACK: empty
        super.processJoin(call);
    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {
        //STACK: ?owner? ?args?
        final Type ownerType = Type.getObjectType(owner);
        final int targetOwnerId = registry.registerClassName(owner);
        final int targetNameId = registry.registerFieldOrMethodName(name);
        //STACK: ?owner? ?args?
        Type[] args = Type.getArgumentTypes(desc);
        int[] locals = InstrumentationUtils.storeArgsToLocals(mv, desc);
        //STACK: ?owner?
        int ownerLocal;
        if (opcode == INVOKESTATIC) {
            mv.push(ownerType);
            ownerLocal = mv.newLocal(Constants.CLASS_TYPE);
            mv.storeLocal(ownerLocal, Constants.CLASS_TYPE);
        } else {
            ownerLocal = mv.newLocal(ownerType);
            mv.storeLocal(ownerLocal, ownerType);
        }
        //STACK: EMPTY
        int accumulatorLocal = mv.newLocal(Type.BOOLEAN_TYPE);
        mv.push(false);
        mv.storeLocal(accumulatorLocal, Type.BOOLEAN_TYPE);
        List<Integer> receiveVertices = new ArrayList<Integer>();
        //STACK: EMPTY
        for (int vertexId : potentialVertices) {
            final HBVertex vertex = HBManager.getInstance().getHappensBeforeVertex(vertexId);
            if (vertex.getType() == SynchronizationPointType.SEND) {
                Label notHbContractLabel = mv.newLabel();
                performCheck(opcode, vertex, ownerType, ownerLocal, name);
                //STACK: boolean
                mv.ifZCmp(IFEQ, notHbContractLabel);
                //STACK: empty
                mv.push(true);
                mv.storeLocal(accumulatorLocal, Type.BOOLEAN_TYPE);
                //STACK: empty
                processManualSynchronization(vertex, args, locals, ownerLocal, targetOwnerId, targetNameId);
                //TODO SEND vertices always return TRUE???
                mv.mark(notHbContractLabel);
            } else {
                performCheck(opcode, vertex, ownerType, ownerLocal, name);
                int local = mv.newLocal(Type.BOOLEAN_TYPE);
                receiveVertices.add(local);
                Label notHbContractLabel = mv.newLabel();
                Label checkDone = mv.newLabel();
                mv.ifZCmp(IFEQ, notHbContractLabel);
                mv.push(true);
                mv.storeLocal(accumulatorLocal, Type.BOOLEAN_TYPE);
                mv.push(true);
                mv.storeLocal(local, Type.BOOLEAN_TYPE);
                mv.goTo(checkDone);
                mv.mark(notHbContractLabel);
                mv.push(false);
                mv.storeLocal(local, Type.BOOLEAN_TYPE);
                mv.mark(checkDone);
            }
        }
        //STACK: EMPTY
        Label notRealHB = mv.newLabel();
        Label executed = mv.newLabel();
        mv.loadLocal(accumulatorLocal, Type.BOOLEAN_TYPE);
        mv.ifZCmp(IFEQ, notRealHB);
        //real hb
        if (opcode != INVOKESTATIC) {
            mv.loadLocal(ownerLocal, ownerType);
        }
        InstrumentationUtils.loadArgsFromLocals(mv, locals);
        callIfHB.run();
        mv.goTo(executed);
        mv.mark(notRealHB);
        if (opcode != INVOKESTATIC) {
            mv.loadLocal(ownerLocal, ownerType);
        }
        InstrumentationUtils.loadArgsFromLocals(mv, locals);
        callIfNotHB.run();
        mv.mark(executed);
        //STACK: EMPTY
        notRealHB = mv.newLabel();
        mv.loadLocal(accumulatorLocal, Type.BOOLEAN_TYPE);
        mv.ifZCmp(IFEQ, notRealHB);
        //real hb => should raise flag
        int counter = 0;
        for (int vertexId : potentialVertices) {
            final HBVertex vertex = HBManager.getInstance().getHappensBeforeVertex(vertexId);
            if (vertex.getType() == SynchronizationPointType.RECEIVE || vertex.getType() == SynchronizationPointType.FULL) {
                Label notHbContractLabel = mv.newLabel();
                Label doneHbContractProcessing = mv.newLabel();
                Label returnedFalse = null;
                if (vertex.isShouldReturnTrue()) {
                    //assume that method returned boolean
                    returnedFalse = mv.newLabel();
                    mv.dup();
                    mv.ifZCmp(IFEQ, returnedFalse);
                    //InstrumentationUtils.skip("hbVertexReturnedTrueCounter", mv);
                }
                mv.loadLocal(receiveVertices.get(counter++));
                mv.ifZCmp(IFEQ, notHbContractLabel);
                processManualSynchronization(vertex, args, locals, ownerLocal, targetOwnerId, targetNameId);
                mv.mark(notHbContractLabel);
                mv.goTo(doneHbContractProcessing);
                if (vertex.isShouldReturnTrue()) {
                    mv.mark(returnedFalse);
                    //InstrumentationUtils.skip("hbVertexReturnedFalseCounter", mv);
                    mv.goTo(doneHbContractProcessing);
                }
                mv.mark(doneHbContractProcessing);
            }
        }
        mv.mark(notRealHB);
        super.processPossibleVertex(potentialVertices, opcode, owner, name, desc, callIfHB, callIfNotHB);
    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {
        //STACK ?owner? args
        int[] args = InstrumentationUtils.storeArgsToLocals(mv, desc);
        //STACK ?owner?
        if (opcode == INVOKESTATIC) {
            mv.push(Constants.ClockedType);
            mv.checkCast(Constants.CLASS_TYPE);
            mv.push(Type.getObjectType(owner));
            mv.checkCast(Constants.CLASS_TYPE);
            mv.invokeVirtual(Constants.CLASS_TYPE, new Method("isAssignableFrom", Type.BOOLEAN_TYPE, new Type[]{Constants.CLASS_TYPE}));
        } else {
            mv.dup();
            mv.instanceOf(Constants.ClockedType);
        }
        final Label clocked = mv.newLabel();
        final Label done = mv.newLabel();
        mv.visitJumpInsn(IFNE, clocked);

        //so, it's not clocked => we should intercept method call as a foreign call
        //STACK ?owner?
        final DRDConfig.ContractInfo methodContractInfo = DRDConfigManager.getConfig().getMethodContractInfo(owner, name);
        switch (methodContractInfo.callType) {
            case WRITE:
                //STACK ?owner?
                if (opcode == INVOKESTATIC) {
                    foreignWriteStatic(owner, name, line, methodContractInfo.raceDetectionMode);
                } else {
                    foreignWriteVirtual(owner, name, line, methodContractInfo.raceDetectionMode);
                }
                //STACK ?owner?
                InstrumentationUtils.loadArgsFromLocals(mv, args);
                call.run();
                mv.goTo(done);
                break;
            case READ: {
                //STACK ?owner?
                if (opcode == INVOKESTATIC) {
                    InstrumentationUtils.loadArgsFromLocals(mv, args);
                    call.run();
                    foreignReadStatic(owner, name, line, methodContractInfo.raceDetectionMode);
                } else {
                    mv.dup();
                    InstrumentationUtils.loadArgsFromLocals(mv, args);
                    call.run();
                    final Type returnType = Type.getReturnType(desc);
                    if (returnType != Type.VOID_TYPE) {
                        mv.swap(Type.getObjectType(owner), returnType);
                    }
                    foreignReadVirtual(owner, name, line, methodContractInfo.raceDetectionMode);
                }
                //STACK: ?ret_val?
                mv.goTo(done);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
        mv.mark(clocked);
        InstrumentationUtils.loadArgsFromLocals(mv, args);
        call.run();
        mv.mark(done);
        super.processForeignCall(opcode, owner, name, desc, line, call);
    }

    public void processVolatile(int opcode, String owner, String name, String desc) {
        final Type ownerType = Type.getObjectType(owner);
        final Type fieldType = Type.getType(desc);
        if (opcode == Opcodes.PUTSTATIC) {
            volatileWriteStatic(owner, name);
            mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
        } else if (opcode == Opcodes.PUTFIELD) {
            mv.swap(ownerType, fieldType);
            volatileWrite(owner, name);
            mv.swap(fieldType, ownerType);
            mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
        } else if (opcode == Opcodes.GETSTATIC) {
            mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
            volatileReadStatic(owner, name);
        } else if (opcode == Opcodes.GETFIELD) {
            mv.dup();
            mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
            mv.swap(ownerType, fieldType);
            volatileRead(owner, name);
        } else throw new IllegalStateException("Unexpected opcode " + opcode + " in visitFieldInsn method");
        super.processVolatile(opcode, owner, name, desc);
    }

    public void processMonitorEnter() {
        //STACK: <monitor owner>
        monitorEnter();
        //STACK: empty
        super.processMonitorEnter();
    }

    public void processMonitorExit() {
        //STACK: <monitor owner>
        mv.dup();
        monitorExit();
        //STACK: <monitor owner>
        mv.monitorExit();
        super.processMonitorExit();
    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        final Type ownerType = Type.getObjectType(owner);
        final Type fieldType = Type.getType(desc);
        int targetOwnerId = registry.registerClassName(owner);
        int targetNameId = registry.registerFieldOrMethodName(name);
        final int locationId = registry.registerLocation(targetOwnerId, targetNameId, ownerId, nameId, line);
        //MSU 2018
			  if(fieldType.getSort() == Type.BOOLEAN || fieldType.getSort() == Type.INT)
			  {
				  this.mv.visitFieldInsn(opcode, owner, name, desc);
				  super.processFieldAccess(opcode, owner, name, desc, line);
				  return;
			  }
        switch (opcode) {
            case GETFIELD:
                //stack: ..., owner
                mv.dup();
                mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
                //stack: ..., owner, return_value
                mv.swap(ownerType, fieldType);
                //stack: ..., return_value, owner
                mv.dup();
                mv.getField(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                final Label nonNullVC = new Label();
                mv.ifNonNull(nonNullVC);
                //stack: ..., return_value, owner
                mv.dup();
                createNewDataClock();
                //stack: ..., return_value, owner, owner, clock
                mv.putField(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                mv.visitLabel(nonNullVC);
                //stack: ..., return_value, owner
                readField(owner, name, locationId);
                //stack: ..., field
                //Done. Top stack value is result of GETFIELD instruction
                break;
            case GETSTATIC:
                //stack: ...
                mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
                //stack: ..., value
                mv.getStatic(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                final Label nonNullVC3 = new Label();
                mv.ifNonNull(nonNullVC3);
                //stack: ..., value
                createNewDataClock();
                mv.putStatic(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                mv.visitLabel(nonNullVC3);
                //stack: ..., value
                //At this moment field clock are not null
                readFieldStatic(owner, name, locationId);
                //stack: ..., value
                //Done. Top stack value is result of GETSTATIC instruction
                break;
            case PUTFIELD:
                //stack: ..., owner, value
                mv.swap(ownerType, fieldType);
                //stack: ..., value, owner
                mv.dup();
                //stack: ..., value, owner, owner
                mv.getField(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                //stack: ..., value, owner, vc
                final Label nonNullVC2 = new Label();
                mv.ifNonNull(nonNullVC2);
                //stack: ..., value, owner
                mv.dup();
                //stack: ..., value, owner, owner
                createNewDataClock();
                //stack: ..., value, owner, owner, vc
                mv.putField(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                //stack: ..., value, owner
                mv.visitLabel(nonNullVC2);
                //stack: ..., value, owner
                mv.visitInsn(InstrumentationUtils.dupX1(fieldType, ownerType));
                //stack: ..., owner, value, owner
                mv.swap(fieldType, ownerType);
                //stack: ..., owner, owner, value
                mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
                writeField(owner, name, locationId);
                //stack: ...
                //Done. Value was put.
                break;
            case PUTSTATIC:
                //stack: ..., value
                mv.getStatic(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                final Label nonNullVC4 = new Label();
                mv.ifNonNull(nonNullVC4);
                //stack: ..., value
                createNewDataClock();
                mv.putStatic(ownerType, name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
                mv.visitLabel(nonNullVC4);
                //stack: ..., value
                //At this moment field clock are not null
                mv.visitFieldInsn(opcode, owner, name, desc); //source instruction
                writeFieldStatic(owner, name, locationId);
                //stack: ...
                //Done. Value was put.
                break;
            default:
                mv.visitFieldInsn(opcode, owner, name, desc);
        }
        super.processFieldAccess(opcode, owner, name, desc, line);
    }

    /**
     * STACK: ..., --> ..., boolean
     */
    private void performCheck(int opcode, HBVertex vertex, Type ownerType, int ownerLocal, String name) {
        InstrumentationUtils.pushDataProvider(mv);
        if (opcode == INVOKESTATIC) {
            mv.push(ownerType);
        } else {
            mv.loadLocal(ownerLocal, ownerType);
            mv.invokeVirtual(Constants.OBJECT_TYPE, GET_CLASS_METHOD);
        }
        mv.push(name);
        mv.push(vertex.getId());
        mv.invokeInterface(Constants.DRDDataProviderType, InstrumentationUtils.MATCHES_TO_HB_VERTEX);
    }

    private void processManualSynchronization(HBVertex vertex, Type[] args, int[] locals, int ownerLocal, int ownerId, int nameId) {
        //STACK: EMPTY
        getAbstractWeakDisposableSample(vertex.getId());
        //STACK: ..., weak_disposable
        mv.dup();
        mv.push(ownerId);
        mv.putField(Constants.ABSTRACT_WEAK_DISPOSABLE_TYPE, "ownerId", Type.INT_TYPE);
        mv.dup();
        mv.push(nameId);
        mv.putField(Constants.ABSTRACT_WEAK_DISPOSABLE_TYPE, "nameId", Type.INT_TYPE);
        mv.dup();
        mv.push(vertex.getHbContractId());
        mv.invokeVirtual(Constants.ABSTRACT_WEAK_DISPOSABLE_TYPE, new Method("setUniqueSyncKey", Type.VOID_TYPE, new Type[]{Type.INT_TYPE}));
        //STACK: ..., weak_disposable
        List<Integer> indices = vertex.getArgsIndices();
        for (int i = 0, size = indices.size(); i < size; i++) {
            int index = indices.get(i);
            mv.dup();
            Type type;
            if (index != -1) {
                type = args[index];
                mv.loadLocal(locals[index]);
            } else {
                type = Constants.OBJECT_TYPE;
                mv.loadLocal(ownerLocal);
            }
            if (!InstrumentationUtils.isPrimitive(type)) {
                type = Constants.OBJECT_TYPE;
            }
            mv.push(i);
            mv.invokeVirtual(Constants.ABSTRACT_WEAK_DISPOSABLE_TYPE, new Method("add" + InstrumentationUtils.toString(type),
                    Type.VOID_TYPE, new Type[]{type, Type.INT_TYPE}));
        }

        //STACK: ..., weak_disposable
        manualSynchronization(ownerId, nameId, vertex);
        //STACK: ... (the same as at the enter of method)
    }


    /*===================================================================*/
    /******************** Calls of interceptor methods *******************/
    /*===================================================================*/

    /**
     * STACK: ..., owner -> ...
     */
    private void readField(String owner, String name, int locationId) {
        fieldOperation(owner, name, locationId, InterceptorMethod.FIELD_READ);
    }

    /**
     * STACK: ..., owner -> ...
     */
    private void writeField(String owner, String name, int locationId) {
        fieldOperation(owner, name, locationId, InterceptorMethod.FIELD_WRITE);
    }

    /**
     * STACK: ..., owner -> ...
     */
    private void readFieldStatic(String owner, String name, int locationId) {
        fieldOperationStatic(owner, name, locationId, InterceptorMethod.FIELD_READ);
    }

    /**
     * STACK: ..., owner -> ...
     */
    private void writeFieldStatic(String owner, String name, int locationId) {
        fieldOperationStatic(owner, name, locationId, InterceptorMethod.FIELD_WRITE);
    }

    private void fieldOperation(String owner, String name, int locationId, InterceptorMethod method) {
        mv.getField(Type.getObjectType(owner), name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(locationId);
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldTrackFieldAccess(owner, name, methodOwner));
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintDataOperation(owner, name, methodOwner));
        InstrumentationUtils.invoke(mv, method);
    }

    /**
     * STACK: ... -> ...
     */
    private void fieldOperationStatic(String owner, String name, int locationId, InterceptorMethod method) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.getStatic(Type.getObjectType(owner), name + Constants.VC_SUFFIX, Constants.IDATACLOCK_TYPE);
        mv.push(locationId);
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldTrackFieldAccess(owner, name, methodOwner));
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintDataOperation(owner, name, methodOwner));
        InstrumentationUtils.invoke(mv, method);
    }

    /**
     * STACK: ... -> ... clock
     */
    public void createNewDataClock() {
        InstrumentationUtils.pushDataProvider(mv);
        mv.push(ownerId);
        mv.invokeInterface(Constants.DRDDataProviderType, InstrumentationUtils.CREATE_NEW_DATA_CLOCK);
    }

    /**
     * STACK: ..., owner -> ..., owner
     */
    private void beforeWait() {
        mv.dup();
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(ownerId);
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintSyncOperation(methodOwner, methodName));
        InstrumentationUtils.invoke(mv, InterceptorMethod.BEFORE_WAIT);
    }

    /**
     * STACK: ..., owner -> ...
     */
    private void afterWait() {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(ownerId);
        mv.push(DRDProperties.soutEnabled  || DRDConfigManager.getConfig().shouldPrintSyncOperation(methodOwner, methodName));
        InstrumentationUtils.invoke(mv, InterceptorMethod.AFTER_WAIT);
    }

    /**
     * STACK: ..., thread -> ...
     */
    private void join() {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        InstrumentationUtils.invoke(mv, InterceptorMethod.THREAD_JOIN);
    }


    /**
     * STACK: ... -> ...
     *
     * @param owner foreign method classname
     * @param name  foreign method name
     * @param line  line number of the call instruction
     */
    private void foreignReadStatic(String owner, String name, int line, DRDConfig.RaceDetectionMode raceDetectionMode) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.push(Type.getObjectType(owner));
        foreignCall(owner, name, line, raceDetectionMode, InterceptorMethod.FOREIGN_READ);
    }

    /**
     * STACK: ... -> ...
     *
     * @param owner foreign method classname
     * @param name  foreign method name
     * @param line  line number of the call instruction
     */
    private void foreignWriteStatic(String owner, String name, int line, DRDConfig.RaceDetectionMode raceDetectionMode) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.push(Type.getObjectType(owner));
        foreignCall(owner, name, line, raceDetectionMode, InterceptorMethod.FOREIGN_WRITE);
    }

    /**
     * STACK: ..., owner -> ...
     *
     * @param owner foreign method classname
     * @param name  foreign method name
     * @param line  line number of the call instruction
     */
    private void foreignReadVirtual(String owner, String name, int line, DRDConfig.RaceDetectionMode raceDetectionMode) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        foreignCall(owner, name, line, raceDetectionMode, InterceptorMethod.FOREIGN_READ);
    }

    /**
     * STACK: ..., owner -> ..., owner
     *
     * @param owner foreign method classname
     * @param name  foreign method name
     * @param line  line number of the call instruction
     */
    private void foreignWriteVirtual(String owner, String name, int line, DRDConfig.RaceDetectionMode raceDetectionMode) {
        mv.dup();
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        foreignCall(owner, name, line, raceDetectionMode, InterceptorMethod.FOREIGN_WRITE);
    }

    /**
     * STACK: ..., awd -> ...
     *
     * @param ownerId method class owner id
     * @param nameId  method name id
     * @param vertex  happens-before vertex
     */
    private void manualSynchronization(int ownerId, int nameId, HBVertex vertex) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(ownerId);
        mv.push(nameId);
        mv.push(DRDProperties.soutEnabled);
        switch (vertex.getType()) {
            case SEND:
                InstrumentationUtils.invoke(mv, InterceptorMethod.MANUAL_SYNC_SEND);
                break;
            case RECEIVE:
                InstrumentationUtils.invoke(mv, InterceptorMethod.MANUAL_SYNC_RECEIVE);
                break;
            case FULL:
                InstrumentationUtils.invoke(mv, InterceptorMethod.MANUAL_SYNC_FULL);
                break;
            default:
                throw new IllegalArgumentException("Unknown SP type : " + vertex.getType());
        }
    }

    /**
     * STACK: ... -> ...
     *
     * @param vertexId id of happens-before vertex
     */
    public void getAbstractWeakDisposableSample(int vertexId) {
        InstrumentationUtils.pushDataProvider(mv);
        mv.push(vertexId);
        mv.invokeInterface(Constants.DRDDataProviderType, InstrumentationUtils.GET_WEAK_DISPOSABLE_SAMPLE);
    }

    /**
     * STACK: ..., interceptor, owner -> ...
     */
    private void foreignCall(String owner, String name, int line, DRDConfig.RaceDetectionMode raceDetectionMode, InterceptorMethod method) {
        int targetOwnerId = registry.registerClassName(owner);
        int targetNameId = registry.registerFieldOrMethodName(name);
        int locationId = registry.registerLocation(targetOwnerId, targetNameId, ownerId, nameId, line);
        boolean shouldTrack = DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldTrackForeignCall(owner, name, methodOwner);
        boolean shouldPrint = DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintDataOperation(owner, name, methodOwner);
        mv.push(registry.registerClassName(methodOwner + " (" + owner + ")"));
        mv.push(locationId);
        mv.push(shouldTrack);
        mv.push(shouldPrint);
        mv.push(raceDetectionMode.equals(DRDConfig.RaceDetectionMode.WRITE_WRITE_ONLY));
        InstrumentationUtils.invoke(mv, method);
    }

    /**
     * STACK: ..., monitor_owner -> ...
     */
    private void monitorEnter() {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(ownerId);
        mv.push(nameId);
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintSyncOperation(methodOwner, methodName));
        InstrumentationUtils.invoke(mv, InterceptorMethod.MONITOR_ENTER);
    }

    /**
     * STACK: ..., monitor_owner -> ...
     */
    private void monitorExit() {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        mv.push(ownerId);
        mv.push(nameId);
        mv.push(DRDProperties.soutEnabled || DRDConfigManager.getConfig().shouldPrintSyncOperation(methodOwner, methodName));
        InstrumentationUtils.invoke(mv, InterceptorMethod.MONITOR_EXIT);
    }

    /**
     * STACK: ... -> ...
     *
     * @param owner field owner class
     * @param name  field name
     */
    private void volatileReadStatic(String owner, String name) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.push(Type.getObjectType(owner));
        volatileOperation(owner, name, InterceptorMethod.VOLATILE_READ);
    }

    /**
     * STACK: ... -> ...
     *
     * @param owner field owner class
     * @param name  field name
     */
    private void volatileWriteStatic(String owner, String name) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.push(Type.getObjectType(owner));
        volatileOperation(owner, name, InterceptorMethod.VOLATILE_WRITE);
    }

    /**
     * STACK: ..., owner -> ...
     *
     * @param owner field owner class
     * @param name  field name
     */
    private void volatileRead(String owner, String name) {
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        volatileOperation(owner, name, InterceptorMethod.VOLATILE_READ);
    }

    /**
     * STACK: ..., owner -> ..., owner
     *
     * @param owner field owner class
     * @param name  field name
     */
    private void volatileWrite(String owner, String name) {
        mv.dup();
        InstrumentationUtils.pushInterceptor(mv);
        mv.swap();
        volatileOperation(owner, name, InterceptorMethod.VOLATILE_WRITE);
    }

    private void volatileOperation(String owner, String name, InterceptorMethod method) {
        int targetOwnerId = registry.registerClassName(owner);
        int targetNameId = registry.registerFieldOrMethodName(name);
        mv.push(targetOwnerId);
        mv.push(targetNameId);
        mv.push(ownerId);
        mv.push(nameId);
        mv.push(DRDProperties.soutEnabled);
        InstrumentationUtils.invoke(mv, method);
    }


    private void loadThisOrClass() {
        if (!isStatic) {
            mv.loadThis();
        } else {
            mv.push(Type.getObjectType(methodOwner));
        }
    }
}
