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

import com.devexperts.drd.bootstrap.DRDEntryPoint;
import com.devexperts.drd.bootstrap.DRDRegistry;
import com.devexperts.drd.transformer.instrument.EmptyVisitor;
import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

class UsageAnalyzer extends EmptyVisitor {
    final Map<String, Boolean> fields;
    int classVersion;
    private String className;
    private int ownerId;
    private boolean isInterface;
    private DRDRegistry registry = DRDEntryPoint.getRegistry();
    private boolean detectRaces;

    UsageAnalyzer(boolean detectRaces) {
        this.detectRaces = detectRaces;
        fields = detectRaces ? new HashMap<String, Boolean>() : null;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        ownerId = registry.registerClassName(className);
        if ((access & Opcodes.ACC_ENUM) != 0) {
            registry.registerEnum(ownerId);
        }
        isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
        classVersion = version & InstrumentationUtils.MAJOR_VERSION_MASK;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (detectRaces) {
            fields.put(name, (access & Opcodes.ACC_STATIC) != 0);
        }
        final int fieldNameId = registry.registerFieldOrMethodName(name);
        if ((access & Opcodes.ACC_FINAL) != 0 || isInterface) {
            registry.registerFinalField(ownerId, fieldNameId);
        } else if ((access & Opcodes.ACC_VOLATILE) != 0) {
            registry.registerVolatileField(ownerId, fieldNameId);
        } else {
            if (detectRaces) {
                registry.registerUsualField(ownerId, fieldNameId);
            }
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return detectRaces ? new MethodUsageAnalyzer(null) : null;
    }

    private class MethodUsageAnalyzer extends MethodVisitor {
        public MethodUsageAnalyzer(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals(className)) {
                fields.put(name, opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
            }
        }
    }
}
