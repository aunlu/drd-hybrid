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

import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static com.devexperts.drd.transformer.instrument.Constants.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Instruments {@link java.lang.ClassLoader} so that it become available to
 * <ul>
 * <li>load classes, that start with "drd". Those are not instrumented copies of certain java classes (principally, classes from java.util.concurrent package, that are used by DRD internals;</li>
 * <li>load composite keys, that are dynamically generated and managed by {@link com.devexperts.drd.transformer.instrument.CompositeKeysManager} based on hb-config.</li>
 * </ul>
 */
//TODO cleanup & merge with com.devexperts.drd.transformer.instrument.system.ClassLoadersDelegateUpInstrumentor
public class ClassLoaderLoadSyntheticInstrumentor extends ClassVisitor {
    public ClassLoaderLoadSyntheticInstrumentor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (LOAD_CLASS_METHOD_RESOLVE.equals(new Method(name, desc))) {
            return new LoadClassMethodInstrumentor(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private class LoadClassMethodInstrumentor extends LoggingGeneratorAdapter {
        private LoadClassMethodInstrumentor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        public void visitCode() {
            //logClassLoading("Loading ");
            super.loadArg(0);
            InstrumentationUtils.startsWith(this, SYNTHETIC_DRD_PREFIX);
            //STACK: boolean
            final Label ignore = super.newLabel();
            super.visitJumpInsn(Opcodes.IFEQ, ignore);
            //STACK: empty
            //load source class
            super.loadThis();
            super.loadArg(0);
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("findLoadedClass", CLASS_TYPE, new Type[]{STRING_TYPE}));
            //STACK: class
            super.dup();
            final Label loaded = super.newLabel();
            super.ifNonNull(loaded);
            //STACK: class
            super.pop();
            //STACK: empty
            super.loadThis();
            super.loadArg(0);
            super.push(SYNTHETIC_DRD_PREFIX);
            //STACK: this string string
            super.invokeVirtual(STRING_TYPE, new Method("length", Type.INT_TYPE, new Type[0]));
            //STACK: this string int
            super.invokeVirtual(STRING_TYPE, new Method("substring", STRING_TYPE, new Type[]{Type.INT_TYPE}));
            //STACK: this string
            super.invokeVirtual(CLASSLOADER_TYPE,
                    new Method("loadClass", CLASS_TYPE, new Type[]{STRING_TYPE}));
            //STACK: class
            super.pop();
            //TODO???
            //STACK: empty

            //load our class
            super.loadThis();
            super.loadArg(0);
            super.invokeStatic(DRD_AGENT_BOOTSTRAP_TYPE, new Method("getNotInstrumentedClassBytes",
                    BYTE_ARRAY_TYPE, new Type[]{CLASSLOADER_TYPE, STRING_TYPE}));
            //STACK: <byte[]>
            super.loadThis();
            super.swap();
            //STACK: <this> <byte[]>
            super.loadArg(0);
            super.swap();
            //STACK: <this> <className> <byte[]>
            super.dup();
            //STACK: <this> <className> <byte[]> <byte[]>
            super.arrayLength();
            super.push(0);
            super.swap();
            //STACK: <this> <className> <byte[]> 0 length
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("defineClass", CLASS_TYPE, new Type[]{
                    STRING_TYPE, BYTE_ARRAY_TYPE, Type.INT_TYPE, Type.INT_TYPE}));
            //STACK: <class>
            super.dup();
            super.loadThis();
            super.swap();
            //STACK: <class> <this> <class>
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("resolveClass", Type.VOID_TYPE, new Type[]{CLASS_TYPE}));
            //STACK: <class>
            super.mark(loaded);
            //logClassLoading("Loaded ");
            super.returnValue();
            super.mark(ignore);

            super.loadArg(0);
            InstrumentationUtils.startsWith(this, COMPOSITE_KEY_PREFIX);
            //STACK: boolean
            final Label ignore2 = super.newLabel();
            super.visitJumpInsn(Opcodes.IFEQ, ignore2);
            //STACK: empty
            final Label notLoaded = super.newLabel();
            super.loadThis();
            super.loadArg(0);
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("findLoadedClass", CLASS_TYPE, new Type[]{STRING_TYPE}));
            super.dup();
            super.ifNull(notLoaded);
            super.returnValue();
            super.mark(notLoaded);
            super.pop();

            //STACK: empty

            super.loadThis();
            super.dup();
            super.loadArg(0);
            //STACK: <this> <this> <className>
            super.loadArg(0);
            InstrumentationUtils.getCompositeKeyBytes(this);
            //STACK: <this> <this> <className> <byte[]>
            super.dup();
            //STACK: <this> <this> <className> <byte[]> <byte[]>
            super.arrayLength();
            super.push(0);
            super.swap();
            //STACK: <this> <this> <className> <byte[]> 0 length
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("defineClass", CLASS_TYPE, new Type[]{
                    STRING_TYPE, BYTE_ARRAY_TYPE, Type.INT_TYPE, Type.INT_TYPE}));
            //STACK: <this> <class>
            super.dupX1();
            //STACK: <class> <this> <class>
            super.invokeVirtual(CLASSLOADER_TYPE, new Method("resolveClass", Type.VOID_TYPE, new Type[]{CLASS_TYPE}));
            //STACK: <class>
            super.returnValue();
            super.mark(ignore2);
            //logClassLoading("Loading default ");
            super.visitCode();
        }

/*        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case ARETURN:
                case RETURN:
                case FRETURN:
                case DRETURN:
                case IRETURN:
                case LRETURN:
                    logClassLoading("Loaded default ");
                    break;
                case ATHROW:
                    logClassLoading(" EXCEPTION LOADING");
                    break;
            }
            super.visitInsn(opcode);
        }*/
    }

    private class LoggingGeneratorAdapter extends GeneratorAdapter {
        private LoggingGeneratorAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
        }

        protected void logClassLoading(String prefix) {
            prepare(prefix);
            InstrumentationUtils.log(this);
        }

        protected void errorClassLoading(String prefix) {
            prepare(prefix);
            InstrumentationUtils.errorWithStackTrace(this);
        }

        private void prepare(String prefix) {
            //STACK: empty
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            //STACK: sb
            mv.visitInsn(DUP);
            //STACK: sb sb
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            //STACK: sb
            mv.visitVarInsn(ALOAD, 0);
            //STACK: sb cl
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            //STACK: sb class
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            //STACK: sb string
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            //STACK: sb
            mv.visitLdcInsn(" : ");
            //STACK: sb string
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            //STACK: sb
            mv.visitLdcInsn(prefix);
            //STACK: sb string
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            //STACK: sb
            mv.visitVarInsn(ALOAD, 1);
            //STACK: sb string
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            //STACK: sb
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            //STACK: string
        }
    }
}
