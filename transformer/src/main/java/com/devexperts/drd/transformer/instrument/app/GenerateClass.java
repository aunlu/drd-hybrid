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
import com.devexperts.drd.transformer.instrument.Constants;
import org.objectweb.asm.ClassVisitor;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class GenerateClass extends ClassTransformer {
    private Map<String, Boolean> fields;

    public GenerateClass(ClassVisitor cv, Map<String, Boolean> fields) {
        super(cv, true);
        this.fields = fields;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ((access & ACC_ANNOTATION) != 0) {
            super.visit(version, access, name, signature, superName, interfaces);
            return;
        }
        boolean isAlreadyClocked = isAlreadyClocked(interfaces);
        String[] newInterfaces;
        if (interfaces == null) {
            newInterfaces = new String[]{Constants.ClockedType.getInternalName()};
        } else if (!isAlreadyClocked) {
            newInterfaces = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
            newInterfaces[newInterfaces.length - 1] = Constants.ClockedType.getInternalName();
        } else {
            newInterfaces = new String[interfaces.length];
            System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
        }
        super.visit(version, access, name, signature, superName, newInterfaces);
        if ((access & ACC_INTERFACE) == 0) {
            for (Map.Entry<String, Boolean> field : fields.entrySet()) {
                if (!DRDEntryPoint.getRegistry().isFinal(DRDEntryPoint.getRegistry().registerClassName(name),
                        DRDEntryPoint.getRegistry().registerFieldOrMethodName(field.getKey()))) {
                    int accessVC = ACC_PUBLIC | ACC_VOLATILE;
                    accessVC |= field.getValue() ? ACC_STATIC : ACC_TRANSIENT;
                    super.visitField(accessVC, field.getKey() + Constants.VC_SUFFIX, Constants.IDATACLOCK_DESC, null, null);
                }
            }
        }
    }

    private static boolean isAlreadyClocked(String[] interfaces) {
        for (String iface : interfaces) {
            if (iface.equalsIgnoreCase(Constants.ClockedType.getInternalName())) return true;
        }
        return false;
    }
}
