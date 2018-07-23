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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

class FrameClassWriter extends ClassWriter {
    // internalClassName -> ClassInfo
    private final ClassLoader loader;
    private final ClassInfoCache ciCache;

    FrameClassWriter(ClassReader classReader, ClassLoader loader, ClassInfoCache ciCache) {
        super(classReader, ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        this.loader = loader;
        this.ciCache = ciCache;
    }

    /**
     * The reason of overriding is to avoid ClassCircularityError which occurs during processing of classes related
     * to java.util.TimeZone and use cache of ClassInfo.
     */
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        ClassInfo c = ciCache.getOrBuildRequiredClassInfo(type1, loader);
        ClassInfo d = ciCache.getOrBuildRequiredClassInfo(type2, loader);

        if (c.isAssignableFrom(d, ciCache, loader))
            return type1;
        if (d.isAssignableFrom(c, ciCache, loader))
            return type2;

        if (c.isInterface() || d.isInterface()) {
            return Constants.OBJECT_TYPE.getInternalName();
        } else {
            do {
                c = c.getSuperclassInfo(ciCache, loader);
            } while (c != null && !c.isAssignableFrom(d, ciCache, loader));
            return c == null ? Constants.OBJECT_TYPE.getInternalName() : c.getInternalName();
        }
    }
}
