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
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.DRDTransformer;
import com.devexperts.drd.transformer.instrument.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class SystemTransformer extends CachingTransformer implements DRDTransformer {
    private final ClassNameFilter remapFilter;
    private final CopyTransformer copyTransformer;
    private final ClassLoader agentClassLoader;

    private final Remapper remapper = new Remapper() {
        @Override
        public String map(String typeName) {
            return remapFilter.acceptSrcClassName(typeName) ?
                    remapFilter.getModifiedClassName(typeName) : typeName;
        }
    };
    private final ClassVisitorFactory remapperFactory = new ClassVisitorFactory() {
        public ClassVisitor createClassVisitor(ClassWriter cw) {
            return new RemappingClassAdapter(cw, remapper);
        }
    };
    private final ClassVisitorFactory classLoaderTransformerFactory = new ClassVisitorFactory() {
        public ClassVisitor createClassVisitor(ClassWriter cw) {
            return new ClassLoaderLoadSyntheticInstrumentor(cw);
        }
    };
    private final ClassVisitorFactory classLoaderFixFactory = new ClassVisitorFactory() {
        public ClassVisitor createClassVisitor(ClassWriter cw) {
            return new ClassLoadersDelegateUpInstrumentor(cw);
        }
    };

    public SystemTransformer(ClassNameFilter remapFilter, ClassLoader agentClassLoader, ClassInfoCache ciCache) {
        super("SystemTransformer", ciCache);
        this.remapFilter = remapFilter;
        this.agentClassLoader = agentClassLoader;
        this.copyTransformer = new CopyTransformer(remapFilter, remapper);
    }

    public byte[] generateNotInstrumentedCopy(String className, byte[] classFileBuffer) {
        return copyTransformer.transform(className, classFileBuffer);
    }

    @Override
    protected byte[] doTransform(ClassLoader loader, String className, byte[] classfileBuffer, boolean retransform) {
        byte[] result = null;
        if (className.equals("java/lang/ClassLoader")) {
            DRDLogger.debug("Transformed java/lang/ClassLoader");
            result = transform(className, classfileBuffer, loader, classLoaderTransformerFactory);
        } else if (loader == agentClassLoader) {
            result = transform(className, classfileBuffer, loader, remapperFactory);
        } else if (!className.startsWith("javax/crypto")){
            //TODO remove!
            //avoid class circularity error
            ClassInfo clInfo = ciCache.getOrBuildRequiredClassInfo("java/lang/ClassLoader", loader);
            try {
                if (clInfo.isAssignableFrom(ciCache.getOrBuildRequiredClassInfo(className, loader), ciCache, loader)) {
                    DRDLogger.debug("Transformed classloader " + className);
                    result = transform(className, classfileBuffer, loader, classLoaderFixFactory);
                }
            } catch (ClassInfoNotLoadedException e) {
                DRDLogger.log("System transformer couldn't detect if " + className + " is a class loader: " + e.getMessage());
            }
        }
        return result;
    }
}
