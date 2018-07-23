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

package com.devexperts.drd.bootstrap;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

public class DRDAgentBootstrap {
    private static final List<String> AGENT_JARS = Arrays.asList("agent.jar", "drd-race.jar");
    private static final List<String> TRANSFORMER_JARS = Arrays.asList(
            "asm-debug-all.jar",
            "transformer.jar",
            "xmlpull.jar",
            "xpp3_min.jar",
            "xstream.jar"
    );

    private static Filter<String> jucFilter = new Filter<String>() {
        public boolean accept(String s) {
            return s.startsWith("java/util/concurrent") || s.startsWith("java.util.concurrent");
        }
    };
    private static ClassNameFilter classNamesFilter = new ClassNameFilter("drd", jucFilter);
    private static ITransformation transformation;
    private static ClassLoader rootClassLoader;
    private static final Map<String, List<String>> skipped = new HashMap<String, List<String>>();

    public static void premain(String options, Instrumentation ins) throws Exception {
        new Throwable("Fake").printStackTrace(BlackHole.BLACK_HOLE);
        try {
            DRDLogger.setLogDir(DRDProperties.getLogDir());
            DRDLogger.log(DRDProperties.dumpSettings());
            DRDLogger.debug("Executing bootstrap sequence ...");
            rootClassLoader = obtainRootClassLoader();
            DRDLogger.debug("Root class loader: " + rootClassLoader.getClass().getName());
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            List<URL> agentUrls = new ArrayList<URL>();
            for (String jar : AGENT_JARS) {
                URL url = cl.getResource(jar);
                agentUrls.add(url);
            }
            JarClassLoader agentCl = new JarClassLoader(agentUrls, "AgentCL");
            List<URL> transformerUrls = new ArrayList<URL>();
            for (String jar : TRANSFORMER_JARS) {
                URL url = cl.getResource(jar);
                transformerUrls.add(url);
            }
            DRDLogger.debug("1/9. Agent classloader initialized.");
            JarClassLoader transformerCL = new JarClassLoader(transformerUrls, "TransformerCL");
            Class tc = transformerCL.loadClass("com.devexperts.drd.transformer.Transformation");
            DRDLogger.debug("2/9. Transformer classloader initialized.");
            Constructor<ITransformation> c = tc.getConstructor(ClassNameFilter.class, ClassLoader.class, ClassLoader.class);
            transformation = c.newInstance(classNamesFilter, agentCl, transformerCL);
            DRDLogger.debug("3/9. ITransformation instance obtained and configured.");
            ins.addTransformer(transformation.getSystemTransformer(), true);
            DRDLogger.debug("4/9. System transformer installed.");
            retransform2(ins);
            DRDLogger.debug("5/9. All classes retransformed by system transformer");
            DRDEntryPoint.setCompositeKeysManager(transformation.getCompositeKeysManager());
            transformation.getCompositeKeysManager().cacheSamples();
            DRDLogger.debug("6/9. CompositeKeysManager installed.");
            agentCl.loadClass("com.devexperts.drd.agent.AgentInitializer").getMethod("setup", ITransformation.class).invoke(null, transformation);
            DRDLogger.debug("7/9. Agent initialized.");
            if (DRDProperties.debugTransformMode == DRDProperties.DebugTransformMode.APPLICATION) {
                ins.addTransformer(transformation.getApplicationTransformer(), true);
                DRDLogger.debug("8/9. Application transformer installed.");
                ins.retransformClasses(ClassLoader.class);
                retransform(ins);
                DRDLogger.debug("9/9. All classes retransformed by application transformer");
            } else {
                DRDLogger.log("7-9/9. DEBUG MODE: Application transformer not installed");
            }
            finish();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void retransform2(Instrumentation ins) {
        Class[] classes = ins.getAllLoadedClasses();
        for (Class clazz : classes) {
            if (clazz.isArray()) continue; //ignore arrays
            try {
                ins.retransformClasses(clazz);
            } catch (UnmodifiableClassException e) {
                DRDLogger.error("Failed to retransform " + clazz.getName(), e);
            }
        }
    }

    private static void retransform(Instrumentation ins) {
        Class[] classes = ins.getAllLoadedClasses();
        for (Class clazz : classes) {
            String name = clazz.getName();
            if (clazz.isArray()) continue; //ignore arrays
            if (clazz == ClassLoader.class) continue; //retransformed earlier
            if (RetransformFilter.INSTANCE.accept(name)) {
                try {
                    ins.retransformClasses(clazz);
                } catch (UnmodifiableClassException e) {
                    DRDLogger.error("Failed to retransform " + name, e);
                }
            } else {
                CollectionUtils.put(getGrouper(name), name, skipped);
            }
        }
        StringBuilder sb = new StringBuilder("Post-retransformation done. Skipped:\n");
        for (Map.Entry<String, List<String>> entry : skipped.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\n");
        }
        DRDLogger.log(sb.append("------------------------------------------------------").toString());
    }

    private static String getGrouper(String name) {
        int fromIndex = name.indexOf('.');
        if (fromIndex < 0) {
            return name;
        }
        fromIndex = name.indexOf('.', fromIndex + 1);
        return fromIndex < 0 ? name : name.substring(0, fromIndex);
    }

    private static void finish() {
        String version = Package.getPackage("com.devexperts.drd.bootstrap").getImplementationVersion();
        DRDLogger.log("\n\n=====================================================\n" +
                "DRD " + (version == null ? "version unknown" : version) + " started it's work\n" +
                "Java " + System.getProperty("java.version") + " from " + System.getProperty("java.home") +
                "\n=====================================================\n");
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("Unhandled exception in thread " + t.getName() + "(tid=" + t.getId() + ").");
                DRDLogger.error("Unhandled exception in thread " + t.getName() + "(tid=" + t.getId() + ").");
                e.printStackTrace();
            }
        });
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static byte[] getNotInstrumentedClassBytes(ClassLoader cl, String className) {
        String srcName = classNamesFilter.getSourceClassName(className);
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(srcName.replace(".", "/") + ".class");
        try {
            FastByteBuffer buf = new FastByteBuffer();
            buf.readFrom(is);
            byte[] bytes = transformation.getSystemTransformer().generateNotInstrumentedCopy(className, buf.getBytes());
            cl.loadClass(srcName);
            return bytes;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader obtainRootClassLoader() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader parent;
        while ((parent = cl.getParent()) != null) {
            cl = parent;
        }
        return cl;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }
}
