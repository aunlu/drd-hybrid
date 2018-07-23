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

import com.devexperts.drd.bootstrap.ClassNameFilter;
import com.devexperts.drd.bootstrap.UnsafeHolder;
import com.devexperts.drd.transformer.instrument.Constants;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import sun.misc.Unsafe;

public class CopyTransformer {
    public static final Type REFLECTION_METHOD_TYPE = Type.getType(java.lang.reflect.Method.class);
    private final ClassNameFilter classNameFilter;
    private final Remapper remapper;

    public CopyTransformer(ClassNameFilter classNameFilter, Remapper remapper) {
        this.classNameFilter = classNameFilter;
        this.remapper = remapper;
    }

    public byte[] transform(String className, byte[] classFileBuffer) {
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new NativeCallsAnalyzer(cw), ClassReader.EXPAND_FRAMES);
        byte[] result = cw.toByteArray();

        cr = new ClassReader(result);
        cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new UnsecureUnsafeReceiveClassVisitor(cw, remapper), ClassReader.EXPAND_FRAMES);
        result = cw.toByteArray();

        cr = new ClassReader(result);
        cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new RemappingVisitor(cw, classNameFilter), ClassReader.EXPAND_FRAMES);
        result = cw.toByteArray();

        InstrumentationUtils.writeTransformedFile(className, result, "_juc_copies");

        return result;
    }

    private class NativeCallsAnalyzer extends ClassVisitor {
        public NativeCallsAnalyzer(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_NATIVE) != 0) {
                if (name.equals("VMSupportsCS8")) {
                    return null;
                } else throw new RuntimeException("Unsupported native method : " +
                        name + " with desc " + desc);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class RemappingVisitor extends ClassVisitor {
        private final ClassNameFilter classNameFilter;

        public RemappingVisitor(ClassVisitor cv, ClassNameFilter classNameFilter) {
            super(Opcodes.ASM5, cv);
            this.classNameFilter = classNameFilter;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null) {
                mv = new NativeCallsRemover(mv, access, name, desc);
            }
            return mv;
        }

        private class NativeCallsRemover extends GeneratorAdapter {
            private NativeCallsRemover(MethodVisitor mv, int access, String name, String desc) {
                super(Opcodes.ASM5, mv, access, name, desc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (!name.equals("VMSupportsCS8")) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                } else {
                    if (classNameFilter.acceptModifiedClassName(owner)) {
                        owner = classNameFilter.getSourceClassName(owner);
                        Type ownerType = Type.getObjectType(owner);
                        super.push(ownerType);
                        super.checkCast(Type.getType(Class.class));
                        super.push(name);
                        super.visitInsn(Opcodes.ACONST_NULL);
                        super.invokeVirtual(Type.getType(Class.class), new Method("getDeclaredMethod",
                                REFLECTION_METHOD_TYPE,
                                new Type[]{Constants.STRING_TYPE, Type.getType(Class[].class)}));
                        super.dup();
                        super.push(true);
                        super.invokeVirtual(REFLECTION_METHOD_TYPE,
                                new Method("setAccessible", Type.VOID_TYPE, new Type[]{Type.BOOLEAN_TYPE}));
                        super.visitInsn(Opcodes.ACONST_NULL);
                        super.visitInsn(Opcodes.ACONST_NULL);
                        super.invokeVirtual(REFLECTION_METHOD_TYPE,
                                new Method("invoke", Constants.OBJECT_TYPE,
                                        new Type[]{Constants.OBJECT_TYPE, Type.getType(Object[].class)}));
                        super.checkCast(Type.getType(Boolean.class));
                        super.unbox(Type.BOOLEAN_TYPE);
                    }
                }
            }
        }
    }

    private static class UnsecureUnsafeReceiveClassVisitor extends RemappingClassAdapter {
        private static final Type UNSAFE_TYPE = Type.getType(Unsafe.class);
        private static final Type UNSAFE_HOLDER_TYPE = Type.getType(UnsafeHolder.class);

        public UnsecureUnsafeReceiveClassVisitor(ClassWriter cw, Remapper r) {
            super(Opcodes.ASM5, cw, r);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null) {
                mv = new UnsafeMethodReflector(mv, access, name, desc);
            }
            return mv;
        }

        private class UnsafeMethodReflector extends GeneratorAdapter {
            public UnsafeMethodReflector(MethodVisitor mv, int access, String name, String desc) {
                super(Opcodes.ASM5, mv, access, name, desc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKESTATIC && InstrumentationUtils.areInputArgsSame(desc,
                        Type.getMethodDescriptor(UNSAFE_TYPE)) && name.equals("getUnsafe")) {
                    super.getStatic(UNSAFE_HOLDER_TYPE, "UNSAFE", UNSAFE_TYPE);
                } else super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
