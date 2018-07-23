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

package com.devexperts.drd.transformer.instrument.system;

import com.devexperts.drd.transformer.instrument.Constants;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static com.devexperts.drd.transformer.instrument.Constants.*;

/**
 * Instruments all class loaders except {@link java.lang.ClassLoader} to delegate loading of drd synthetic classes to {@link ClassLoader#getSystemClassLoader()}
 */
public class ClassLoadersDelegateUpInstrumentor extends ClassVisitor {
    public static final Method GET_ROOT_CLASS_LOADER_METHOD = new Method("getRootClassLoader",
            Constants.CLASSLOADER_TYPE, Constants.EMPTY_TYPE_ARRAY);

    public ClassLoadersDelegateUpInstrumentor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Constants.LOAD_CLASS_METHOD.equals(new Method(name, desc))) {
            return new LoadClassMethodInstrumentor(false, cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        } else if (LOAD_CLASS_METHOD_RESOLVE.equals(new Method(name, desc))) {
            return new LoadClassMethodInstrumentor(true, cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private class LoadClassMethodInstrumentor extends GeneratorAdapter {
        private boolean resolve;

        private LoadClassMethodInstrumentor(boolean resolve, MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
            this.resolve = resolve;
        }

        public void visitCode() {
            //debug("load@ " + (resolve ? "res" : "us") + ": ", "");
            super.loadArg(0);
            InstrumentationUtils.startsWith(this, Constants.COMPOSITE_KEY_PREFIX);
            super.loadArg(0);
            InstrumentationUtils.startsWith(this, Constants.DRD_SYNTHETIC_CLASSES_PREFIX);
            super.visitInsn(Opcodes.IOR);
            Label usualSequence = new Label();
            super.visitJumpInsn(Opcodes.IFEQ, usualSequence);
            super.loadThis();
            super.invokeStatic(DRD_AGENT_BOOTSTRAP_TYPE, GET_ROOT_CLASS_LOADER_METHOD);
            super.visitJumpInsn(Opcodes.IF_ACMPEQ, usualSequence);
            super.invokeStatic(DRD_AGENT_BOOTSTRAP_TYPE, GET_ROOT_CLASS_LOADER_METHOD);
            super.loadArg(0);
            super.invokeVirtual(Constants.CLASSLOADER_TYPE, Constants.LOAD_CLASS_METHOD);
            if (resolve) {
                super.loadArg(1);
                Label notResolve = super.newLabel();
                super.visitJumpInsn(Opcodes.IFEQ, notResolve);
                debug("Resolve manually: ", "");
                super.dup();
                super.loadThis();
                super.swap();
                super.invokeVirtual(CLASSLOADER_TYPE, new Method("resolveClass", Type.VOID_TYPE, new Type[]{CLASS_TYPE}));
                debug("Resolved ", "");
                super.mark(notResolve);
            }
            super.returnValue();
            super.mark(usualSequence);
            super.visitCode();
        }

        private void debug(String prefix, String suffix) {
            prepare(prefix, suffix);
            InstrumentationUtils.debug(this);
        }

        private void log(String prefix, String suffix) {
            prepare(prefix, suffix);
            InstrumentationUtils.log(this);
        }

        private void prepare(String prefix, String suffix) {
            super.push(prefix);
            super.loadArg(0);
            super.invokeVirtual(Constants.STRING_TYPE, new Method("concat", Constants.STRING_TYPE, new Type[]{Constants.STRING_TYPE}));
            super.push(suffix);
            super.invokeVirtual(Constants.STRING_TYPE, new Method("concat", Constants.STRING_TYPE, new Type[]{Constants.STRING_TYPE}));
        }
    }
}
