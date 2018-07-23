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

import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import com.devexperts.drd.transformer.instrument.InterceptorMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import static org.objectweb.asm.Opcodes.*;

public class ClassLoaderTrackFreezer extends ClassVisitor {
    public ClassLoaderTrackFreezer(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("getClassLoadingLock")) {
            return new Instrumentor(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private class Instrumentor extends GeneratorAdapter {
        private Instrumentor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        public void visitCode() {
            InstrumentationUtils.pushInterceptor(this);
            InstrumentationUtils.invoke(this, InterceptorMethod.LOCK_HARD);
            super.pop();
            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            //unlock
            switch (opcode) {
                case ARETURN:
                case RETURN:
                case FRETURN:
                case DRETURN:
                case IRETURN:
                case LRETURN:
                case ATHROW:
                    InstrumentationUtils.pushInterceptor(this);
                    InstrumentationUtils.invoke(this, InterceptorMethod.UNLOCK_HARD);
                    super.pop();
                    break;
            }
            super.visitInsn(opcode);
        }
    }
}
