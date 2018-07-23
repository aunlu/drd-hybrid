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

import com.devexperts.drd.transformer.instrument.Constants;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class ThreadInstrumentor extends ClassVisitor {
    public ThreadInstrumentor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("run") && desc.equals(Constants.VOID_METHOD_DESCRIPTOR)) {
            return new RunMethodInstrumentor(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        } else if (name.equals("start") && desc.equals(Constants.VOID_METHOD_DESCRIPTOR)) {
            return new StartMethodInstrumentor(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        } else if (name.equals("setName")) {
            return new SetNameMethodInstrumentor(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private static class RunMethodInstrumentor extends GeneratorAdapter {
        public RunMethodInstrumentor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                super.invokeStatic(Constants.DRDEntryPointType, InstrumentationUtils.GET_INTERCEPTOR_METHOD);
                super.invokeInterface(Constants.DRDInterceptorType, Constants.DRDInterceptorDie);
            }
            super.visitInsn(opcode);
        }
    }

    private static class StartMethodInstrumentor extends GeneratorAdapter {
        public StartMethodInstrumentor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        public void visitCode() {
            super.invokeStatic(Constants.DRDEntryPointType, InstrumentationUtils.GET_INTERCEPTOR_METHOD);
            super.loadThis();
            super.invokeInterface(Constants.DRDInterceptorType, Constants.DRDInterceptorStart);
            //register thread name
            super.invokeStatic(Constants.DRDEntryPointType, InstrumentationUtils.GET_REGISTRY_METHOD);
            super.loadThis();
            super.invokeInterface(Constants.DRDRegistryType, new Method("registerThread", Type.VOID_TYPE, new Type[]{Constants.THREAD_TYPE}));
            super.visitCode();
        }
    }
    private static class SetNameMethodInstrumentor extends GeneratorAdapter {
        public SetNameMethodInstrumentor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        public void visitCode() {
            //register thread name
            super.invokeStatic(Constants.DRDEntryPointType, InstrumentationUtils.GET_REGISTRY_METHOD);
            super.loadThis();
            super.invokeInterface(Constants.DRDRegistryType, new Method("registerThread", Type.VOID_TYPE, new Type[]{Constants.THREAD_TYPE}));
            super.visitCode();
        }
    }
}
