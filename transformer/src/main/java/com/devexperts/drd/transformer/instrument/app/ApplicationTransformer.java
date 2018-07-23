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
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.transformer.config.DRDConfigManager;
import com.devexperts.drd.transformer.config.InstrumentationScopeConfig;
import com.devexperts.drd.transformer.instrument.CachingTransformer;
import com.devexperts.drd.transformer.instrument.ClassInfoCache;
import com.devexperts.drd.transformer.instrument.ClassVisitorFactory;
import com.devexperts.drd.transformer.instrument.Constants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

public class ApplicationTransformer extends CachingTransformer {
    private final ClassVisitorFactory threadTransformerFactory = new ClassVisitorFactory() {
        public ClassVisitor createClassVisitor(ClassWriter cw) {
            return new ThreadInstrumentor(cw);
        }
    };
    private final ClassVisitorFactory classLoaderTransformerFactory = new ClassVisitorFactory() {
        public ClassVisitor createClassVisitor(ClassWriter cw) {
            return new ClassLoaderTrackFreezer(cw);
        }
    };
    private final ClassLoader transformerCL;

    public ApplicationTransformer(ClassLoader transformerCL, ClassInfoCache ciCache) {
        super("ApplicationTransformer", ciCache);
        this.transformerCL = transformerCL;
        DRDLogger.log("Config initialized : " + (DRDConfigManager.getConfig() != null) + ".");
        DRDLogger.log("Transformer and detector launched.");
    }

    public byte[] doTransform(ClassLoader loader, String className, byte[] classfileBuffer, boolean retransform) {
        if (className.startsWith("com/devexperts/drd/tests/examples/") && className.endsWith("TestRunner")) {
            DRDLogger.log("\n\n-------------------------------\nModel example: " + className + "\n------------------------------------\n");
        }
        TransformationMode mode = loader == transformerCL ? TransformationMode.INTERNAL_IGNORE : null;
        if (mode != null) {
            DRDLogger.debug(mode + " (CL_BASED) " + className);
        } else {
            mode = getMode(className);
            if (retransform && mode == TransformationMode.DETECT_RACES) {
                //can't detect races in classes being retransformed because it requires adding Clocked interface
                mode = TransformationMode.DETECT_SYNC;
            }
            DRDLogger.debug(mode + " (CALCULATED) " + className);
        }
        byte[] modifiedClassFileBuffer;
        switch (mode) {
            case IGNORE:
            case INTERNAL_IGNORE:
            case APPLICATION_IGNORE:
                return null;
            case THREAD:
                modifiedClassFileBuffer = transform(className, classfileBuffer, loader, threadTransformerFactory);
                break;
            case CLASS_LOADER:
                modifiedClassFileBuffer = transform(className, classfileBuffer, loader, classLoaderTransformerFactory);
                break;
            case DETECT_SYNC:
            case DETECT_RACES: //fall through
                modifiedClassFileBuffer = transformApplicationClass(className, classfileBuffer, mode, loader);
                break;
            default:
                throw new IllegalArgumentException("Unknown mode " + mode);
        }
        DRDEntryPoint.getRegistry().registerInstrumentedClass(className);
        return modifiedClassFileBuffer;
    }

    private byte[] transformApplicationClass(String className, byte[] classfileBuffer, TransformationMode mode, ClassLoader cl) {
        ClassReader cr = new ClassReader(classfileBuffer);
        UsageAnalyzer analyzer = new UsageAnalyzer(mode == TransformationMode.DETECT_RACES);
        cr.accept(analyzer, ClassReader.SKIP_FRAMES + ClassReader.SKIP_DEBUG);
        if (TransformationMode.DETECT_RACES.equals(mode)) {
            cr = new ClassReader(classfileBuffer);
            ClassWriter suidCW = getClassWriter(cl, cr, analyzer.classVersion, className);
            ClassVisitor suidV = new SerialVersionUIDAdder(suidCW);
            cr.accept(suidV, ClassReader.EXPAND_FRAMES);
            byte[] buffer2 = suidCW.toByteArray();

            cr = new ClassReader(buffer2);
            ClassWriter cw = getClassWriter(cl, cr, analyzer.classVersion, className);
            cr.accept(new GenerateClass(cw, analyzer.fields), ClassReader.EXPAND_FRAMES);
            DRDLogger.debug(className + " modified to detect sync events and races.");
            return cw.toByteArray();
        } else if (TransformationMode.DETECT_SYNC.equals(mode)) {
            cr = new ClassReader(classfileBuffer);
            ClassWriter cw = getClassWriter(cl, cr, analyzer.classVersion, className);
            cr.accept(new ClassTransformer(cw, false), ClassReader.EXPAND_FRAMES);
            DRDLogger.debug(className + " modified to detect sync events.");
            return cw.toByteArray();
        } else {
            return null;
        }
    }


    private TransformationMode getMode(String className) {
        if (className.equals("java/lang/Thread")) return TransformationMode.THREAD;
        if (className.equals("java/lang/ClassLoader")) return TransformationMode.CLASS_LOADER;
        if (className.endsWith("package-info")) return TransformationMode.IGNORE;
        if (className.startsWith("java/lang/ThreadLocal")) return TransformationMode.IGNORE;
        if (className.startsWith("java/lang/ref/Reference")) return TransformationMode.IGNORE;
        //Do not instrument DRD classes
        if (className.startsWith(Constants.DRD_SYNTHETIC_CLASSES_PREFIX)) return TransformationMode.INTERNAL_IGNORE;
        if (className.startsWith("com/devexperts/drd") && !className.startsWith("com/devexperts/drd/tests"))
            return TransformationMode.INTERNAL_IGNORE;
        if (InstrumentationScopeConfig.getInstance().shouldInterceptSyncOperations(className)) {
            if (InstrumentationScopeConfig.getInstance().shouldInterceptDataOperations(className)) {
                return TransformationMode.DETECT_RACES;
            } else return TransformationMode.DETECT_SYNC;
        } else return TransformationMode.APPLICATION_IGNORE;
    }
}
