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

import com.devexperts.drd.bootstrap.DRDAgentBootstrap;
import com.devexperts.drd.bootstrap.DRDLogger;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.util.*;

/**
 * Caches class info for each class loader. Reference to class loader is never explicitly stored and is
 * always passed in arguments, so that there is not strong references to it. They are cached using
 * weak references to make class loaders eligible for garbage collection despite this cache.
 */
public class ClassInfoCache {
    // ClassLoader -> internalClassName -> ClassInfo
	private WeakHashMap<ClassLoader, ClassInfoMap> classInfoCache = new WeakHashMap<ClassLoader, ClassInfoMap>();

	// Returns null if not found or failed to load
	public synchronized ClassInfo getOrBuildClassInfo(String internalClassName, ClassLoader loader) {
		ClassInfo classInfo = getOrInitClassInfoMap(loader).get(internalClassName);
		if (classInfo != null)
			return classInfo;
		ClassInfoMap classInfoMap = classInfoCache.get(loader);
		classInfo = buildClassInfo(internalClassName, loader);
		if (classInfo != null)
			classInfoMap.put(internalClassName, classInfo);
		return classInfo;
	}

	// throws RuntimeException if not found or failed to load
	public synchronized ClassInfo getOrBuildRequiredClassInfo(String internalClassName, ClassLoader loader) {
		ClassInfo classInfo = getOrBuildClassInfo(internalClassName, loader);
		if (classInfo == null)
			throw new ClassInfoNotLoadedException("Cannot load class information for " + internalClassName.replace('/', '.'));
		return classInfo;
	}

	synchronized ClassInfoMap getOrInitClassInfoMap(ClassLoader loader) {
		ClassInfoMap classInfoMap = classInfoCache.get(loader);
		if (classInfoMap == null) {
			// make sure we have parent loader's map first
			if (loader != null)
				getOrInitClassInfoMap(loader.getParent());
			// at first time when class loader is discovered, tracked classes in this class loader are cached
			classInfoCache.put(loader, classInfoMap = new ClassInfoMap());
			classInfoMap.doneInit();
		} else
			try {
				classInfoMap.waitInit();
			} catch (InterruptedException e) {
				DRDLogger.log("Interrupted while waiting to initialize tracking classes");
				Thread.currentThread().interrupt();
			}
		return classInfoMap;
	}

	private ClassInfo buildClassInfo(String internalClassName, ClassLoader loader) {
		// check if parent class loader has this class info
		if (loader != null)  {
			ClassInfo classInfo = getOrBuildClassInfo(internalClassName, loader.getParent());
			if (classInfo != null)
				return classInfo;
		}
		// actually build it
		try {
			String classFileName = internalClassName + ".class";
			InputStream in;
			if (loader == null)
				in = DRDAgentBootstrap.class.getResourceAsStream("/" + classFileName);
			else
				in = loader.getResourceAsStream(classFileName);
			if (in == null) {
                return null;
            }
			ClassInfoAnalyzer analyzer = new ClassInfoAnalyzer();
			try {
				ClassReader cr = new ClassReader(in);
				cr.accept(analyzer, ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES + ClassReader.SKIP_CODE);
			} finally {
                in.close();
			}
			return analyzer.getClassInfo();
		} catch (Throwable t) {
            DRDLogger.log("Failed to load class " + internalClassName + " because of exception ", t);
			return null;
		}
	}
}
