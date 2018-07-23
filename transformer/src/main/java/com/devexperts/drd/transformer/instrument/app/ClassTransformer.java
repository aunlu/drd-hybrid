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
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_NATIVE;

public class ClassTransformer extends ClassVisitor {
    private boolean isInterface;
    private boolean isNative;
    private String owner;
    private boolean detectRaces;

    public ClassTransformer(ClassVisitor cv, boolean detectRaces) {
        super(Opcodes.ASM5, cv);
        this.detectRaces = detectRaces;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = (access & ACC_INTERFACE) != 0;
        isNative = (access & ACC_NATIVE) != 0;
        owner = name;
        // roll-forward version to MIN_CLASS_VERSION so that we can use ldc <class-constant> instruction,
        // but keep deprecated flag intact.
        if ((version & InstrumentationUtils.MAJOR_VERSION_MASK) < InstrumentationUtils.MIN_CLASS_VERSION)
            version = InstrumentationUtils.MIN_CLASS_VERSION | (version & Opcodes.ACC_DEPRECATED);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!Constants.CLINIT_METHOD.equals(name) && !Constants.INIT_METHOD.equals(name) && !isInterface && !isNative && mv != null) {
            mv = new SynchronizationTransformer(new GeneratorAdapter(mv, access, name, desc), access, owner, name, detectRaces);
            mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
        }
        return mv;
    }
}
