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

package com.devexperts.drd.transformer.instrument;

import com.devexperts.drd.bootstrap.CollectionUtils;
import com.devexperts.drd.bootstrap.AbstractWeakDisposable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devexperts.drd.transformer.instrument.Constants.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class CompositeKeyGenerator {
    private static final String FIELD_PREFIX = "field";
    private static final Type WEAK_REFERENCE_TYPE = getType(WeakReference.class);
    private static final Type UNSUPPORTED_OPERATION_EXCEPTION_TYPE = getType(UnsupportedOperationException.class);
    private static final String UNIQUE_SYNC_KEY = "uniqueSyncKey";
    private static final String OWNER_ID = "ownerId";
    private static final String NAME_ID = "nameId";
    private static final String HASH_CODE = "hashcode";
    private static final Method IDENTITY_HASH_CODE_METHOD = new Method("identityHashCode", INT_TYPE, new Type[]{OBJECT_TYPE});
    private static final Type STRING_BUILDER_TYPE = getType(StringBuilder.class);
    private static final Method STRING_BUILDER_APPEND_STRING_METHOD = new Method("append", STRING_BUILDER_TYPE, new Type[]{STRING_TYPE});
    private static final Method TO_STRING_METHOD = new Method("toString", STRING_TYPE, EMPTY_TYPE_ARRAY);
    public static final String SAMPLE_SUFFIX = "Sample";

    private final Map<Type, List<Integer>> indices = new HashMap<Type, List<Integer>>();
    private final String name;
    private final Type ownerType;
    private final Type sampleType;
    private final Type[] types;

    public CompositeKeyGenerator(Type[] types, String name) {
        this.name = name;
        ownerType = getObjectType(name);
        sampleType = getObjectType(name + SAMPLE_SUFFIX);
        this.types = types;
    }

    public byte[] generateCompositeKeyBytes() {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        //visit class declaration
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, name, null, ABSTRACT_WEAK_DISPOSABLE_TYPE.getInternalName(), null);

        //visit fields
        for (int i = 0; i < types.length; i++) {
            final Type fieldType = getFieldType(types[i]);
            cw.visitField(ACC_PUBLIC, FIELD_PREFIX + i, fieldType.getDescriptor(), null, null);
            CollectionUtils.put(isPrimitive(fieldType) ? fieldType : OBJECT_TYPE, i, indices);
        }
        if (indices.get(OBJECT_TYPE) == null || indices.get(OBJECT_TYPE).size() == 0) {
            throw new UnsupportedOperationException("No object types in type array " + Arrays.toString(types) + ".");
        }

        //visit <init> method

        final GeneratorAdapter init = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, INIT_METHOD,
                VOID_METHOD_DESCRIPTOR, null, null), ACC_PUBLIC, INIT_METHOD, VOID_METHOD_DESCRIPTOR);
        //invoke superclass constructor
        init.loadThis();
        init.invokeConstructor(getType(AbstractWeakDisposable.class), new Method(INIT_METHOD, VOID_TYPE, EMPTY_TYPE_ARRAY));
        init.loadThis();
        init.push(1);
        init.putField(ownerType, HASH_CODE, INT_TYPE);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        //visit "canDelete" method to implement WeakDisposable interface
        final String canDeleteDescriptor = getMethodDescriptor(BOOLEAN_TYPE);
        final String canDeleteName = "canDelete";
        final GeneratorAdapter canDelete = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, canDeleteName,
                canDeleteDescriptor, null, null), ACC_PUBLIC, canDeleteName, canDeleteDescriptor);
        Label smthNull = canDelete.newLabel();
        for (int index : indices.get(OBJECT_TYPE)) {
            canDelete.loadThis();
            canDelete.getField(ownerType, FIELD_PREFIX + index, WEAK_REFERENCE_TYPE);
            canDelete.invokeVirtual(WEAK_REFERENCE_TYPE, new Method("get", OBJECT_TYPE, EMPTY_TYPE_ARRAY));
            canDelete.ifNull(smthNull);
        }
        canDelete.push(false);
        canDelete.returnValue();
        canDelete.visitLabel(smthNull);
        canDelete.push(true);
        canDelete.returnValue();
        canDelete.visitMaxs(0, 0);
        canDelete.visitEnd();

        //visit "hashcode" method
        final String hashcodeDescriptor = getMethodDescriptor(INT_TYPE);
        final GeneratorAdapter hashcode = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, HASH_CODE,
                hashcodeDescriptor, null, null), ACC_PUBLIC, HASH_CODE, hashcodeDescriptor);
        hashcode.loadThis();
        hashcode.getField(ownerType, HASH_CODE, INT_TYPE);
        hashcode.returnValue();
        hashcode.visitMaxs(0, 0);
        hashcode.visitEnd();

        //visit "compareToCompositeKey" method
        generateCompareKeyToKeyMethod(cw);
        //visit "compareToCompositeKeySample" method
        generateCompareKeyToSampleMethod(cw);

        //visit "equals" method
        final String equalsDescriptor = getMethodDescriptor(BOOLEAN_TYPE, OBJECT_TYPE);
        final String equalsName = "equals";
        final GeneratorAdapter equals = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, equalsName,
                equalsDescriptor, null, null), ACC_PUBLIC, equalsName, equalsDescriptor);
        Label maybeSample = equals.newLabel();
        Label returnFalse = equals.newLabel();

        Label notSame = equals.newLabel();
        equals.loadThis();
        equals.loadArg(0);
        equals.visitJumpInsn(IF_ACMPNE, notSame);
        equals.push(true);
        equals.returnValue();
        equals.mark(notSame);

        equals.loadArg(0);
        equals.ifNull(returnFalse);
        equals.loadArg(0);
        equals.instanceOf(ownerType);
        equals.visitJumpInsn(IFEQ, maybeSample);
        equals.loadThis();
        equals.loadArg(0);
        equals.checkCast(ownerType);
        equals.invokeVirtual(ownerType, new Method("compareToCompositeKey", BOOLEAN_TYPE, new Type[]{ownerType}));
        equals.returnValue();

        equals.visitLabel(maybeSample);
        equals.loadArg(0);
        equals.instanceOf(sampleType);
        equals.visitJumpInsn(IFEQ, returnFalse);
        equals.loadThis();
        equals.loadArg(0);
        equals.checkCast(sampleType);
        equals.invokeVirtual(ownerType, new Method("compareToCompositeKeySample", BOOLEAN_TYPE, new Type[]{sampleType}));
        equals.returnValue();

        equals.visitLabel(returnFalse);
        equals.push(false);
        equals.returnValue();
        equals.visitMaxs(0, 0);
        equals.visitEnd();

        //generate addXXX methods
        generateCompositeKeyAddMethod(BOOLEAN_TYPE, cw);
        generateCompositeKeyAddMethod(BYTE_TYPE, cw);
        generateCompositeKeyAddMethod(SHORT_TYPE, cw);
        generateCompositeKeyAddMethod(CHAR_TYPE, cw);
        generateCompositeKeyAddMethod(INT_TYPE, cw);
        generateCompositeKeyAddMethod(LONG_TYPE, cw);
        generateCompositeKeyAddMethod(FLOAT_TYPE, cw);
        generateCompositeKeyAddMethod(DOUBLE_TYPE, cw);
        generateCompositeKeyAddMethod(OBJECT_TYPE, cw);

        //generate newInstance method
        final String descriptor = getMethodDescriptor(ABSTRACT_WEAK_DISPOSABLE_TYPE);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "newInstance",
                descriptor, null, null), ACC_PUBLIC, "newInstance", descriptor);
        mv.throwException(UNSUPPORTED_OPERATION_EXCEPTION_TYPE, "newInstance() method is not supported for " + name);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        //generate copy method
        final String copyDescriptor = getMethodDescriptor(ABSTRACT_WEAK_DISPOSABLE_TYPE);
        final GeneratorAdapter copyMV = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "copy",
                copyDescriptor, null, null), ACC_PUBLIC, "copy", copyDescriptor);
        copyMV.throwException(UNSUPPORTED_OPERATION_EXCEPTION_TYPE, "copy() method is not supported for " + name);
        copyMV.visitMaxs(0, 0);
        copyMV.visitEnd();

        final String toStringDescriptor = getMethodDescriptor(STRING_TYPE);
        final GeneratorAdapter toStringMV = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "toString",
                toStringDescriptor, null, null), ACC_PUBLIC, "toString", toStringDescriptor);
        toStringMV.newInstance(STRING_BUILDER_TYPE);
        toStringMV.dup();
        toStringMV.invokeConstructor(STRING_BUILDER_TYPE, new Method(INIT_METHOD, VOID_METHOD_DESCRIPTOR));
        toStringMV.loadThis();
        toStringMV.invokeConstructor(ABSTRACT_WEAK_DISPOSABLE_TYPE, TO_STRING_METHOD);
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        toStringMV.push(" (" + name + ") ");
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        for (int i = 0; i < types.length; i++) {
            final Type fieldType = isPrimitive(types[i]) ? types[i] : OBJECT_TYPE;
            toStringMV.push(InstrumentationUtils.toString(fieldType));
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
            toStringMV.push(" : ");
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
            if (fieldType == OBJECT_TYPE) {
                toStringMV.push("object hashcode=");
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
                toStringMV.loadThis();
                toStringMV.getField(ownerType, FIELD_PREFIX + i, fieldType);
                toStringMV.invokeVirtual(Constants.OBJECT_TYPE, new Method("hashCode", INT_TYPE, EMPTY_TYPE_ARRAY));
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, new Method("append", STRING_BUILDER_TYPE, new Type[]{INT_TYPE}));
            } else {
                toStringMV.loadThis();
                toStringMV.getField(ownerType, FIELD_PREFIX + i, fieldType);
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, new Method("append", STRING_BUILDER_TYPE, new Type[]{fieldType}));
            }
            toStringMV.push("; ");
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        }
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, TO_STRING_METHOD);
        toStringMV.returnValue();
        toStringMV.visitMaxs(0, 0);
        toStringMV.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateCompareKeyToKeyMethod(ClassWriter cw) {
        //instance method, comparing this to arg0
        final String descriptor = getMethodDescriptor(BOOLEAN_TYPE, ownerType);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PRIVATE, "compareToCompositeKey",
                descriptor, null, null), ACC_PRIVATE, "compareToCompositeKey", descriptor);

        Label pop3AndJumpToReturnFalse = mv.newLabel();
        Label pop2AndJumpToReturnFalse = mv.newLabel();
        Label popAndJumpToReturnFalse = mv.newLabel();
        Label returnFalse = mv.newLabel();

        mv.loadArg(0);
        //STACK: that_composite_key

        mv.dup();
        mv.getField(ownerType, HASH_CODE, INT_TYPE);
        mv.loadThis();
        mv.getField(ownerType, HASH_CODE, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        mv.dup();
        mv.getField(ownerType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.loadThis();
        mv.getField(ownerType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        for (int i = 0; i < types.length; i++) {
            final Type realType = getFieldType(types[i]);
            if (!realType.equals(WEAK_REFERENCE_TYPE)) {
                mv.dup();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.loadThis();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.ifCmp(realType, IFNE, popAndJumpToReturnFalse);
            } else {
                mv.dup();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.invokeVirtual(WEAK_REFERENCE_TYPE, new Method("get", OBJECT_TYPE, EMPTY_TYPE_ARRAY));
                mv.dup();
                mv.ifNull(pop2AndJumpToReturnFalse);
                mv.loadThis();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.invokeVirtual(WEAK_REFERENCE_TYPE, new Method("get", OBJECT_TYPE, EMPTY_TYPE_ARRAY));
                mv.dup();
                mv.ifNull(pop3AndJumpToReturnFalse);
                mv.visitJumpInsn(IF_ACMPNE, popAndJumpToReturnFalse);
            }
        }
        mv.pop();
        mv.push(true);

        mv.returnValue();
        mv.visitLabel(pop3AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(pop2AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(popAndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(returnFalse);
        mv.push(false);
        mv.returnValue();
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateCompareKeyToSampleMethod(ClassWriter cw) {
        //instance method, comparing this to arg0
        final String descriptor = getMethodDescriptor(BOOLEAN_TYPE, sampleType);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PRIVATE, "compareToCompositeKeySample",
                descriptor, null, null), ACC_PRIVATE, "compareToCompositeKeySample", descriptor);

        Label pop3AndJumpToReturnFalse = mv.newLabel();
        Label pop2AndJumpToReturnFalse = mv.newLabel();
        Label popAndJumpToReturnFalse = mv.newLabel();
        Label returnFalse = mv.newLabel();

        mv.loadArg(0);
        //STACK: that_composite_key

        mv.dup();
        mv.getField(sampleType, HASH_CODE, INT_TYPE);
        mv.loadThis();
        mv.getField(ownerType, HASH_CODE, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        mv.dup();
        mv.getField(sampleType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.loadThis();
        mv.getField(ownerType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        for (int i = 0; i < types.length; i++) {
            final Type realType = getFieldType(types[i]);
            if (!realType.equals(WEAK_REFERENCE_TYPE)) {
                mv.dup();
                mv.getField(sampleType, FIELD_PREFIX + i, realType);
                mv.loadThis();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.ifCmp(realType, IFNE, popAndJumpToReturnFalse);
            } else {
                mv.dup();
                mv.getField(sampleType, FIELD_PREFIX + i, OBJECT_TYPE);
                mv.dup();
                mv.ifNull(pop2AndJumpToReturnFalse);
                mv.loadThis();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.invokeVirtual(WEAK_REFERENCE_TYPE, new Method("get", OBJECT_TYPE, EMPTY_TYPE_ARRAY));
                mv.dup();
                mv.ifNull(pop3AndJumpToReturnFalse);
                mv.visitJumpInsn(IF_ACMPNE, popAndJumpToReturnFalse);
            }
        }
        mv.pop();
        mv.push(true);

        mv.returnValue();
        mv.visitLabel(pop3AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(pop2AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(popAndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(returnFalse);
        mv.push(false);
        mv.returnValue();
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateCompareSampleToKeyMethod(ClassWriter cw) {
        //instance method, comparing this to arg0
        final String descriptor = getMethodDescriptor(BOOLEAN_TYPE, ownerType);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PRIVATE, "compareToCompositeKey",
                descriptor, null, null), ACC_PRIVATE, "compareToCompositeKey", descriptor);

        Label pop3AndJumpToReturnFalse = mv.newLabel();
        Label pop2AndJumpToReturnFalse = mv.newLabel();
        Label popAndJumpToReturnFalse = mv.newLabel();
        Label returnFalse = mv.newLabel();

        mv.loadArg(0);
        //STACK: that_composite_key

        mv.dup();
        mv.getField(ownerType, HASH_CODE, INT_TYPE);
        mv.loadThis();
        mv.getField(sampleType, HASH_CODE, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        mv.dup();
        mv.getField(ownerType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.loadThis();
        mv.getField(sampleType, UNIQUE_SYNC_KEY, INT_TYPE);
        mv.visitJumpInsn(IF_ICMPNE, popAndJumpToReturnFalse);

        for (int i = 0; i < types.length; i++) {
            final Type realType = getFieldType(types[i]);
            if (!realType.equals(WEAK_REFERENCE_TYPE)) {
                mv.dup();
                mv.getField(ownerType, FIELD_PREFIX + i, realType);
                mv.loadThis();
                mv.getField(sampleType, FIELD_PREFIX + i, realType);
                mv.ifCmp(realType, IFNE, popAndJumpToReturnFalse);
            } else {
                mv.dup();
                mv.getField(ownerType, FIELD_PREFIX + i, WEAK_REFERENCE_TYPE);
                mv.invokeVirtual(WEAK_REFERENCE_TYPE, new Method("get", OBJECT_TYPE, EMPTY_TYPE_ARRAY));
                mv.dup();
                mv.ifNull(pop2AndJumpToReturnFalse);
                mv.loadThis();
                mv.getField(sampleType, FIELD_PREFIX + i, OBJECT_TYPE);
                mv.dup();
                mv.ifNull(pop3AndJumpToReturnFalse);
                mv.visitJumpInsn(IF_ACMPNE, popAndJumpToReturnFalse);
            }
        }
        mv.pop();
        mv.push(true);

        mv.returnValue();
        mv.visitLabel(pop3AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(pop2AndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(popAndJumpToReturnFalse);
        mv.pop();
        mv.visitLabel(returnFalse);
        mv.push(false);
        mv.returnValue();
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateCompositeKeyAddMethod(final Type type, ClassWriter cw) {
        final String descriptor = getMethodDescriptor(VOID_TYPE, type, INT_TYPE);
        final String name = "add" + InstrumentationUtils.toString(type);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, name,
                descriptor, null, null), ACC_PUBLIC, name, descriptor);
        final List<Integer> indices = this.indices.get(type);
        if (indices != null && indices.size() > 0) {
            mv.loadArg(1);
            mv.tableSwitch(toArray(indices), new TableSwitchGenerator() {
                public void generateCase(int key, Label end) {
                    if (isPrimitive(type)) {
                        mv.loadThis();
                        mv.loadArg(0);
                        mv.putField(ownerType, FIELD_PREFIX + key, type);
                    } else {
                        mv.loadThis();
                        mv.newInstance(WEAK_REFERENCE_TYPE);
                        mv.dup();
                        mv.loadArg(0);
                        mv.invokeConstructor(WEAK_REFERENCE_TYPE, new Method(INIT_METHOD, VOID_TYPE, new Type[]{OBJECT_TYPE}));
                        mv.putField(ownerType, FIELD_PREFIX + key, WEAK_REFERENCE_TYPE);
                    }
                    mv.loadThis();
                    mv.loadArg(0);
                    incrementHashCode(mv, type, ownerType);
                    mv.mark(end);
                }

                public void generateDefault() {
                }
            });
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateCompositeKeySampleAddMethod(final Type type, ClassWriter cw) {
        final String descriptor = getMethodDescriptor(VOID_TYPE, type, INT_TYPE);
        final String name = "add" + InstrumentationUtils.toString(type);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, name,
                descriptor, null, null), ACC_PUBLIC, name, descriptor);
        final List<Integer> indices = this.indices.get(type);
        if (indices != null && indices.size() > 0) {
            mv.loadArg(1);
            mv.tableSwitch(toArray(indices), new TableSwitchGenerator() {
                public void generateCase(int key, Label end) {
                    mv.loadThis();
                    mv.loadArg(0);
                    mv.putField(sampleType, FIELD_PREFIX + key, type);
                    mv.loadThis();
                    mv.loadArg(0);
                    incrementHashCode(mv, type, sampleType);
                    mv.mark(end);
                }

                public void generateDefault() {
                }
            });
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private boolean isPrimitive(Type type) {
        switch (type.getSort()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE:
                return true;
            default:
                return false;
        }
    }

    private int[] toArray(List<Integer> list) {
        final int size = list.size();
        int[] res = new int[size];
        for (int i = 0; i < size; i++) res[i] = list.get(i);
        return res;
    }

    /**
     * STACK: ..., arg ---> ...
     */
    private void incrementHashCode(GeneratorAdapter mv, Type type, Type ownerType) {
        if (type.equals(LONG_TYPE)) {
            mv.dup2();
            mv.push(32);
            mv.visitInsn(LSHR);
            mv.visitInsn(LXOR);
            mv.cast(LONG_TYPE, INT_TYPE);
        } else if (!type.equals(INT_TYPE)) {
            if (isPrimitive(type)) {
                mv.box(type);
            }
            mv.checkCast(OBJECT_TYPE);
            mv.invokeStatic(Constants.SYSTEM_TYPE, IDENTITY_HASH_CODE_METHOD);
        }
        mv.loadThis();
        mv.getField(ownerType, HASH_CODE, INT_TYPE);
        mv.push(31);
        mv.math(IMUL, INT_TYPE);
        mv.math(IADD, INT_TYPE);
        mv.putField(ownerType, HASH_CODE, INT_TYPE);
    }

    private Type getFieldType(Type type) {
        return isPrimitive(type) ? type : WEAK_REFERENCE_TYPE;
    }

    public byte[] generateCompositeKeySampleBytes() {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        //visit class declaration
        final String sampleName = name + SAMPLE_SUFFIX;
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, sampleName, null, ABSTRACT_WEAK_DISPOSABLE_TYPE.getInternalName(), null);

        //visit fields
        for (int i = 0; i < types.length; i++) {
            final Type fieldType = isPrimitive(types[i]) ? types[i] : OBJECT_TYPE;
            cw.visitField(ACC_PUBLIC, FIELD_PREFIX + i, fieldType.getDescriptor(), null, null);
            CollectionUtils.put(fieldType, i, indices);
        }
        if (indices.get(OBJECT_TYPE) == null || indices.get(OBJECT_TYPE).size() == 0) {
            throw new UnsupportedOperationException("No object types in type array " + Arrays.toString(types) + ".");
        }

        //visit <init> method
        final GeneratorAdapter init = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, INIT_METHOD,
                VOID_METHOD_DESCRIPTOR, null, null), ACC_PUBLIC, INIT_METHOD, VOID_METHOD_DESCRIPTOR);
        //invoke superclass constructor
        init.loadThis();
        init.invokeConstructor(getType(AbstractWeakDisposable.class), new Method(INIT_METHOD, VOID_TYPE, EMPTY_TYPE_ARRAY));
        init.loadThis();
        init.push(1);
        init.putField(sampleType, HASH_CODE, INT_TYPE);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        //visit "canDelete" method to implement WeakDisposable interface
        final String canDeleteDescriptor = getMethodDescriptor(BOOLEAN_TYPE);
        final String canDeleteName = "canDelete";
        final GeneratorAdapter canDelete = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, canDeleteName,
                canDeleteDescriptor, null, null), ACC_PUBLIC, canDeleteName, canDeleteDescriptor);
        canDelete.throwException(UNSUPPORTED_OPERATION_EXCEPTION_TYPE, "Operation canDelete() is not supported in " + sampleName);
        canDelete.visitMaxs(0, 0);
        canDelete.visitEnd();

        //visit "hashcode" method
        final String hashcodeDescriptor = getMethodDescriptor(INT_TYPE);
        final GeneratorAdapter hashcode = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, HASH_CODE,
                hashcodeDescriptor, null, null), ACC_PUBLIC, HASH_CODE, hashcodeDescriptor);
        hashcode.loadThis();
        hashcode.getField(sampleType, HASH_CODE, INT_TYPE);
        hashcode.returnValue();
        hashcode.visitMaxs(0, 0);
        hashcode.visitEnd();

        //visit "compareToCompositeKeySample" method
        generateCompareSampleToKeyMethod(cw);

        //visit "equals" method
        final String equalsDescriptor = getMethodDescriptor(BOOLEAN_TYPE, OBJECT_TYPE);
        final String equalsName = "equals";
        final GeneratorAdapter equals = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, equalsName,
                equalsDescriptor, null, null), ACC_PUBLIC, equalsName, equalsDescriptor);
        Label maybeKey = equals.newLabel();
        Label returnFalse = equals.newLabel();

        Label notSame = equals.newLabel();
        equals.loadThis();
        equals.loadArg(0);
        equals.visitJumpInsn(IF_ACMPNE, notSame);
        equals.push(true);
        equals.returnValue();
        equals.mark(notSame);

        equals.loadArg(0);
        equals.ifNull(returnFalse);
        equals.loadArg(0);
        equals.instanceOf(sampleType);
        equals.visitJumpInsn(IFEQ, maybeKey);
        canDelete.throwException(UNSUPPORTED_OPERATION_EXCEPTION_TYPE, sampleName + " can't be compared to another " + sampleName);

        equals.visitLabel(maybeKey);
        equals.loadArg(0);
        equals.instanceOf(ownerType);
        equals.visitJumpInsn(IFEQ, returnFalse);
        equals.loadThis();
        equals.loadArg(0);
        equals.checkCast(ownerType);
        equals.invokeVirtual(sampleType, new Method("compareToCompositeKey", BOOLEAN_TYPE, new Type[]{ownerType}));
        equals.returnValue();

        equals.visitLabel(returnFalse);
        equals.push(false);
        equals.returnValue();
        equals.visitMaxs(0, 0);
        equals.visitEnd();

        //generate addXXX methods
        generateCompositeKeySampleAddMethod(BOOLEAN_TYPE, cw);
        generateCompositeKeySampleAddMethod(BYTE_TYPE, cw);
        generateCompositeKeySampleAddMethod(SHORT_TYPE, cw);
        generateCompositeKeySampleAddMethod(CHAR_TYPE, cw);
        generateCompositeKeySampleAddMethod(INT_TYPE, cw);
        generateCompositeKeySampleAddMethod(LONG_TYPE, cw);
        generateCompositeKeySampleAddMethod(FLOAT_TYPE, cw);
        generateCompositeKeySampleAddMethod(DOUBLE_TYPE, cw);
        generateCompositeKeySampleAddMethod(OBJECT_TYPE, cw);

        //generate newInstance method
        final String descriptor = getMethodDescriptor(ABSTRACT_WEAK_DISPOSABLE_TYPE);
        final GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "newInstance",
                descriptor, null, null), ACC_PUBLIC, "newInstance", descriptor);
        mv.newInstance(sampleType);
        mv.dup();
        mv.invokeConstructor(sampleType, new Method(INIT_METHOD, VOID_TYPE, EMPTY_TYPE_ARRAY));
        mv.returnValue();
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        //generate copy method
        final String copyDescriptor = getMethodDescriptor(ABSTRACT_WEAK_DISPOSABLE_TYPE);
        final GeneratorAdapter copyMV = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "copy",
                copyDescriptor, null, null), ACC_PUBLIC, "copy", copyDescriptor);
        copyMV.loadThis();
        copyMV.newInstance(ownerType);
        copyMV.dup();
        copyMV.invokeConstructor(ownerType, new Method(INIT_METHOD, VOID_TYPE, EMPTY_TYPE_ARRAY));
        //stack: owner

        copyMV.dup();
        copyMV.loadThis();
        copyMV.getField(sampleType, UNIQUE_SYNC_KEY, INT_TYPE);
        copyMV.putField(ownerType, UNIQUE_SYNC_KEY, INT_TYPE);

        copyMV.dup();
        copyMV.loadThis();
        copyMV.getField(sampleType, OWNER_ID, INT_TYPE);
        copyMV.putField(ownerType, OWNER_ID, INT_TYPE);

        copyMV.dup();
        copyMV.loadThis();
        copyMV.getField(sampleType, NAME_ID, INT_TYPE);
        copyMV.putField(ownerType, NAME_ID, INT_TYPE);

        copyMV.dup();
        copyMV.loadThis();
        copyMV.getField(sampleType, HASH_CODE, INT_TYPE);
        copyMV.putField(ownerType, HASH_CODE, INT_TYPE);

        for (int i = 0; i < types.length; i++) {
            copyMV.dup();
            final Type fieldType = getFieldType(types[i]);
            if (!WEAK_REFERENCE_TYPE.equals(fieldType)) {
                copyMV.loadThis();
                copyMV.getField(sampleType, FIELD_PREFIX + i, fieldType);
                copyMV.putField(ownerType, FIELD_PREFIX + i, fieldType);
            } else {
                copyMV.newInstance(WEAK_REFERENCE_TYPE);
                copyMV.dup();
                copyMV.loadThis();
                copyMV.getField(sampleType, FIELD_PREFIX + i, OBJECT_TYPE);
                copyMV.invokeConstructor(WEAK_REFERENCE_TYPE, new Method(INIT_METHOD, VOID_TYPE, new Type[]{OBJECT_TYPE}));
                copyMV.putField(ownerType, FIELD_PREFIX + i, WEAK_REFERENCE_TYPE);
            }
        }
        copyMV.returnValue();
        copyMV.visitMaxs(0, 0);
        copyMV.visitEnd();

        final String toStringDescriptor = getMethodDescriptor(STRING_TYPE);
        final GeneratorAdapter toStringMV = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "toString",
                toStringDescriptor, null, null), ACC_PUBLIC, "toString", toStringDescriptor);
        toStringMV.newInstance(STRING_BUILDER_TYPE);
        toStringMV.dup();
        toStringMV.invokeConstructor(STRING_BUILDER_TYPE, new Method(INIT_METHOD, VOID_METHOD_DESCRIPTOR));
        toStringMV.loadThis();
        toStringMV.invokeConstructor(ABSTRACT_WEAK_DISPOSABLE_TYPE, TO_STRING_METHOD);
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        toStringMV.push(" (" + sampleName + ") ");
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        for (int i = 0; i < types.length; i++) {
            final Type fieldType = isPrimitive(types[i]) ? types[i] : OBJECT_TYPE;
            toStringMV.push(InstrumentationUtils.toString(fieldType));
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
            toStringMV.push(" : ");
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
            if (fieldType == OBJECT_TYPE) {
                toStringMV.push("object hashcode=");
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
                toStringMV.loadThis();
                toStringMV.getField(sampleType, FIELD_PREFIX + i, fieldType);
                toStringMV.invokeVirtual(Constants.OBJECT_TYPE, new Method("hashCode", INT_TYPE, EMPTY_TYPE_ARRAY));
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, new Method("append", STRING_BUILDER_TYPE, new Type[]{INT_TYPE}));
            } else {
                toStringMV.loadThis();
                toStringMV.getField(sampleType, FIELD_PREFIX + i, fieldType);
                toStringMV.invokeVirtual(STRING_BUILDER_TYPE, new Method("append", STRING_BUILDER_TYPE, new Type[]{fieldType}));
            }
            toStringMV.push("; ");
            toStringMV.invokeVirtual(STRING_BUILDER_TYPE, STRING_BUILDER_APPEND_STRING_METHOD);
        }
        toStringMV.invokeVirtual(STRING_BUILDER_TYPE, TO_STRING_METHOD);
        toStringMV.returnValue();
        toStringMV.visitMaxs(0, 0);
        toStringMV.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
