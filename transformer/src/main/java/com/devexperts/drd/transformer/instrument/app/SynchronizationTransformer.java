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

package com.devexperts.drd.transformer.instrument.app;

import com.devexperts.drd.agent.core.Guard;
import com.devexperts.drd.bootstrap.DRDEntryPoint;
import com.devexperts.drd.bootstrap.DRDProperties;
import com.devexperts.drd.bootstrap.DRDRegistry;
import com.devexperts.drd.transformer.config.InstrumentationScopeConfig;
import com.devexperts.drd.transformer.config.RaceDetectionType;
import com.devexperts.drd.transformer.config.hb.HBManager;
import com.devexperts.drd.transformer.instrument.Constants;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import com.devexperts.drd.transformer.instrument.InterceptorMethod;
import com.devexperts.drd.transformer.instrument.Processor;
import com.devexperts.drd.transformer.transform.MethodTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("ConstantConditions")
class SynchronizationTransformer extends MethodVisitor {
    private final MethodTransformer transformer;
    private final GeneratorAdapter mv;
    private final String methodOwner;
    private final String methodName;
    private final boolean isSynchronized;
    private final boolean detectRaces;
    private int line = -1;
    private int monitorOwner = -1;
    /**
     * Lock status at the entrance of the method
     */
    private int lockStatusVar = -1;
    private int ownerId;
    private DRDRegistry registry = DRDEntryPoint.getRegistry();

    public SynchronizationTransformer(GeneratorAdapter mv, int access, String owner, String name, boolean detectRaces) {
        super(Opcodes.ASM5, mv);
        this.mv = mv;
        this.methodName = name;
        this.methodOwner = owner;
        ownerId = registry.registerClassName(owner);
        isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.detectRaces = detectRaces;
        this.transformer = TransformerFactory.createTransformer(mv, access, owner, name);
        if (DRDProperties.profilingEnabled) {
            DRDEntryPoint.getStatistics().transformed();
        }
        //this.transformer = new CountingTransformer(mv);
    }

    public void visitCode() {
        //save flag to local variable
        InstrumentationUtils.pushInterceptor(mv);
        InstrumentationUtils.invoke(mv, InterceptorMethod.STATUS);
        lockStatusVar = mv.newLocal(Type.INT_TYPE);
        mv.storeLocal(lockStatusVar, Type.INT_TYPE);
        if (isSynchronized) {
            executeIfNotInHB(new Processor() {
                public void process() {
                    transformer.processEnterSynchronizedMethod();
                }

                public void trackSkip() {
                    transformer.skipEnterSynchronizedMethod();
                }

                public void execute() {
                }
            });
        }
        mv.visitCode();
    }

    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
        final int targetOwnerId = registry.registerClassName(owner);
        if (registry.isEnum(targetOwnerId)) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        final Type ownerType = Type.getObjectType(owner);
        if (name.equals("clone") && ownerType.getSort() == Type.ARRAY && (ownerType.getElementType().getSort() != Type.OBJECT ||
                registry.isEnum(registry.registerClassName(ownerType.getElementType().getInternalName())))) {
            //Ignore call of Lcom/package/Enum;.clone() from com/package/Enum.values()
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        final Runnable simpleCall = new Runnable() {
            public void run() {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
        if (opcode == INVOKEVIRTUAL) {
            if (name.equals("wait") && Constants.OBJECT_TYPE.getInternalName().equals(owner)) {
                executeIfNotInHB(new Processor() {
                    public void process() {
                        transformer.processWait(desc, simpleCall);
                    }

                    public void trackSkip() {
                        transformer.skipWait();
                    }

                    public void execute() {
                        mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                });
                return;
            } else if (name.equals("join") && desc.equals(Constants.VOID_METHOD_DESCRIPTOR)) {
                executeIfNotInHB(new Processor() {
                    public void process() {
                        transformer.processJoin(simpleCall);
                    }

                    public void trackSkip() {
                        transformer.skipJoin();
                    }

                    public void execute() {
                        mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                });
                return;
            }
        }

        //process foreign call, if it's not a vertex of happens-before contract

        final List<Integer> potentialVertices = HBManager.getInstance().getHbVerticesIds(name, desc);
        final boolean isPossibleVertex = potentialVertices != null && potentialVertices.size() > 0;
        final boolean isForeignCall = !InstrumentationScopeConfig.getInstance().shouldInterceptDataOperations(owner);
        final boolean processForeignCall = isForeignCall && detectRaces && opcode != INVOKESPECIAL &&
                InstrumentationScopeConfig.getInstance().shouldDetectRacesOnMethodCall(owner, name);
        executeIfNotInHB(new Processor() {
            public void process() {
                final Runnable nonVertexCall = new Runnable() {
                    public void run() {
                        if (isForeignCall) {
                            if (processForeignCall) {
                                transformer.processForeignCall(opcode, owner, name, desc, line, simpleCall);
                            } else {
                                transformer.skipForeignCall();
                                simpleCall.run();
                            }
                        } else {
                            simpleCall.run();
                        }
                    }
                };
                if (isPossibleVertex) {
                    transformer.processPossibleVertex(potentialVertices, opcode, owner, name, desc, new Runnable() {
                        public void run() {
                            lockSoft(true);
                            mv.visitMethodInsn(opcode, owner, name, desc, itf);
                            lockSoft(false);
                        }
                    }, nonVertexCall);
                } else {
                    nonVertexCall.run();
                }
            }

            public void trackSkip() {
                if (isPossibleVertex) {
                    transformer.skipPossibleVertex();
                } else if (processForeignCall) {
                    transformer.skipForeignCall();
                }
            }

            public void execute() {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    @Override
    public void visitLabel(Label label) {
        //complicated processing of monenter: see thread http://mail-archive.ow2.org/asm/2012-06/msg00020.html
        mv.mark(label);
        if (monitorOwner > 0) {
            executeIfNotInHB(new Processor() {
                public void process() {
                    mv.loadLocal(monitorOwner, Constants.OBJECT_TYPE);
                    transformer.processMonitorEnter();
                }

                public void trackSkip() {
                    transformer.skipMonitorEnter();
                }

                public void execute() {
                }
            });
            monitorOwner = -1;
        }
    }

    public void visitInsn(int opcode) {
        switch (opcode) {
            case MONITORENTER:
                mv.dup();
                mv.monitorEnter();
                mv.checkCast(Constants.OBJECT_TYPE);
                monitorOwner = mv.newLocal(Constants.OBJECT_TYPE);
                mv.storeLocal(monitorOwner, Constants.OBJECT_TYPE);
                break;
            case MONITOREXIT:
                executeIfNotInHB(new Processor() {
                    public void process() {
                        transformer.processMonitorExit();
                    }

                    public void trackSkip() {
                        transformer.skipMonitorExit();
                    }

                    public void execute() {
                        mv.monitorExit();
                    }
                });
                break;
            case ARETURN:
            case RETURN:
            case FRETURN:
            case DRETURN:
            case IRETURN:
            case LRETURN:
            case ATHROW:
                if (isSynchronized) {
                    executeIfNotInHB(new Processor() {
                        public void process() {
                            transformer.processExitSynchronizedMethod();
                        }

                        public void trackSkip() {
                            transformer.skipExitSynchronizedMethod();
                        }

                        public void execute() {
                        }
                    });
                }
                mv.visitInsn(opcode);
                break;
            default:
                mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        int targetOwnerId = registry.registerClassName(owner);
        int targetNameId = registry.registerFieldOrMethodName(name);
        if (registry.isFinal(targetOwnerId, targetNameId) || registry.isEnum(targetOwnerId)) {
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        if (registry.isVolatile(targetOwnerId, targetNameId)) {
            executeIfNotInHB(new Processor() {
                public void process() {
                    transformer.processVolatile(opcode, owner, name, desc);
                }

                public void trackSkip() {
                    transformer.skipVolatile();
                }

                public void execute() {
                    mv.visitFieldInsn(opcode, owner, name, desc);
                }
            });
        } else {
            RaceDetectionType raceDetectionType = shouldDetectRacesOnField(owner, name);
            InstrumentationUtils.trackFieldAccess(raceDetectionType, mv);
            if (detectRaces && shouldDetectRacesOnField(owner, name) == RaceDetectionType.DETECT) {
                transformer.processFieldAccess(opcode, owner, name, desc, line);
            } else {
                transformer.skipFieldAccess();
                mv.visitFieldInsn(opcode, owner, name, desc);
            }
        }
    }

    private RaceDetectionType shouldDetectRacesOnField(String owner, String name) {
        RaceDetectionType type = InstrumentationScopeConfig.getInstance().shouldDetectRacesOnField(methodOwner, owner, name);
        if (type != RaceDetectionType.DETECT) {
            return type;
        }
        final int targetOwnerId = registry.registerClassName(owner);
        final int targetNameId = registry.registerFieldOrMethodName(name);
        if (targetOwnerId != ownerId && !registry.isInstrumented(targetOwnerId)) {
            return RaceDetectionType.IGNORE_FINAL;
        }
        if (targetOwnerId != ownerId && !registry.hasField(targetOwnerId, targetNameId)) {
            return RaceDetectionType.IGNORE_ERROR;
        }
        if (Constants.INIT_METHOD.equals(methodName) || Constants.CLINIT_METHOD.equals(methodName)) {
            return RaceDetectionType.IGNORE_CONSTRUCTOR;
        }
        return type;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.line = line;
        mv.visitLineNumber(line, start);
    }

    /**
     * Creates following code:
     * <pre>
     * {@code
     * if ($currentLockStatusVar$ == 0) { //i.e. guard is available
     *     call.process();
     * } else if ($currentLockStatusVar$ == 1) { //i.e. guard is soft locked
     *     call.skip();
     * } else { //i.e. guard is hard locked
     *     call.execute();
     * }
     * }
     * </pre>
     * Resulting state of the stack would be same as starting (i.e. stack is unmodified by this method from outer perspective).
     *
     * @param call target handlers for both cases: flag is raised, flag is released
     */
    public void executeIfNotInHB(Processor call) {
        Label available = mv.newLabel();
        Label lockedSoft = mv.newLabel();
        Label done = mv.newLabel();
        mv.loadLocal(lockStatusVar, Type.INT_TYPE);
        mv.push(Guard.AVAILABLE);
        mv.ifICmp(IFEQ, available);
        mv.loadLocal(lockStatusVar, Type.INT_TYPE);
        mv.push(Guard.LOCKED_SOFT);
        mv.ifICmp(IFEQ, lockedSoft);
        //InstrumentationUtils.lockedHard(methodOwner + "." + methodName, mv);
        call.execute();
        mv.goTo(done);
        mv.mark(available);
        call.process();
        mv.goTo(done);
        mv.mark(lockedSoft);
        //InstrumentationUtils.lockedSoft(methodOwner + "." + methodName, mv);
        call.skip();
        mv.mark(done);
    }

    private void lockSoft(boolean lock) {
        Label done = new Label();
        mv.loadLocal(lockStatusVar, Type.INT_TYPE);
        mv.push(lock ? Guard.AVAILABLE : Guard.LOCKED_SOFT);
        mv.ifICmp(IFNE, done);
        InstrumentationUtils.pushInterceptor(mv);
        InstrumentationUtils.invoke(mv, lock ? InterceptorMethod.LOCK_SOFT : InterceptorMethod.UNLOCK_SOFT);
        //STACK: new lock status
        mv.storeLocal(lockStatusVar, Type.INT_TYPE);
        mv.mark(done);
    }
}
