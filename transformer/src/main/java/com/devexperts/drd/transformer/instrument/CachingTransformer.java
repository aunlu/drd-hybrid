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

import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.Filter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.FileNotFoundException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public abstract class CachingTransformer implements ClassFileTransformer {
    private final String name;

    private final Filter<String> transformFilter = new Filter<String>() {
        public boolean accept(String s) {
            return s != null && !s.startsWith("com/devexperts/drd/bootstrap") && !s.startsWith(Constants.SYNTHETIC_DRD_PREFIX);
        }
    };
    protected final ClassInfoCache ciCache;

    protected CachingTransformer(String name, ClassInfoCache ciCache) {
        this.name = name;
        this.ciCache = ciCache;
    }

    protected ClassWriter getClassWriter(ClassLoader cl, ClassReader cr, int version, String className) {
        boolean computeFrames = version >= Opcodes.V1_6;
        if (computeFrames) {
            return new FrameClassWriter(cr, cl, ciCache);
        } else {
            DRDLogger.debug(name + " uses slow class writer for " + className);
            return new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            DRDLogger.debug("Transforming " + className + " @ " + name);
            doBeforeTransformation(className, classfileBuffer);
            byte[] result = transformFilter.accept(className) ? doTransform(loader, className, classfileBuffer, classBeingRedefined != null) : null;
            if (result != null) {
                doAfterTransformation(className, result);
                DRDLogger.debug(name + " transformed " + className);
            } else {
                DRDLogger.debug(name + " skipped " + className);
                doAfterTransformation(className, classfileBuffer);
            }
            return result;
        } catch (Throwable e) {
            DRDLogger.error(name + " failed to transform " + className, e);
            return null;
        }
    }

    protected abstract byte[] doTransform(ClassLoader loader, String className, byte[] classfileBuffer, boolean retransform);

    private void doAfterTransformation(String className, byte[] modifiedClassFileBuffer) throws FileNotFoundException {
        InstrumentationUtils.writeTransformedFile(className, modifiedClassFileBuffer, "_after_" + name);
    }

    private void doBeforeTransformation(String className, byte[] classFileBuffer) throws FileNotFoundException {
        InstrumentationUtils.writeTransformedFile(className, classFileBuffer, "_before_" + name);
    }

    protected byte[] transform(String className, byte[] classfileBuffer, ClassLoader cl, ClassVisitorFactory cvp) {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassInfoMap map = ciCache.getOrInitClassInfoMap(cl);
        ClassInfo classInfo = map.get(className);
        if (classInfo == null) {
            ClassInfoAnalyzer analyzer = new ClassInfoAnalyzer();
            cr.accept(analyzer, ClassReader.SKIP_FRAMES + ClassReader.SKIP_DEBUG + ClassReader.SKIP_CODE);
            classInfo = analyzer.getClassInfo();
            map.put(className, classInfo);
        }
        ClassWriter cw = getClassWriter(cl, cr, classInfo.getVersion(), className);
        cr.accept(cvp.createClassVisitor(cw), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
