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

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JarClassLoader extends URLClassLoader {
    /* The context to be used when loading classes and resources */
    private final AccessControlContext acc;
    private final List<CachedClass> classes = Collections.synchronizedList(new ArrayList<CachedClass>());
    private final String name;

    public JarClassLoader(List<URL> urls, String name) throws IOException {
        super(new URL[0]);
        this.name = name;
        acc = AccessController.getContext();
        for (URL url : urls) {
            cacheJar(url);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                // Do nothing
            }
            if (c == null) {
                if (!name.endsWith("ICompositeKeysManager") || !this.name.equals("AgentCL")) {
                    sout(" asks parent for class " + name);
                } else {
                    sout(" asks parent for class " + name);
                    //new Exception("@" + this.name + "@ asks parent for class " + name).printStackTrace();
                }
                c = getParent().loadClass(name);
                sout(" found " + name + " : " + (c != null));
            } else {
                sout(" loaded " + name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        for (CachedClass clazz : classes) {
            if (name.equals(clazz.name)) {
                return clazz.define();
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (CachedClass clazz : classes) {
            if (name.equals(clazz.getResourceName())) {
                return new ByteArrayInputStream(clazz.bytes);
            }
        }
        return super.getResourceAsStream(name);
    }

    private void cacheClass(URL url, Manifest manifest, String name, byte[] bytes, Certificate[] certificates) {
        classes.add(new CachedClass(url, manifest, name, bytes, certificates));
    }

    private void cacheJar(URL url) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(url.openStream());
        Manifest manifest = jarInputStream.getManifest();
        while (true) {
            JarEntry entry = jarInputStream.getNextJarEntry();
            if (entry == null) {
                break;
            }
            String name = entry.getName();
            if (name.endsWith("/")) {
                continue;
            }
            if (!name.endsWith(".class")) {
                continue;
            }
            name = name.replace('/', '.').substring(0, name.length() - 6);
            int size = (int) entry.getSize();
            byte[] bytes;
            if (size > 0) {
                bytes = new byte[size];
                for (int read = 0; read < size; ) {
                    int rd = jarInputStream.read(bytes, read, size - read);
                    if (rd < 0) {
                        throw new EOFException(url.getFile() + ", " + name + ": read " + read + " of " + size);
                    }
                    read += rd;
                }
            } else {
                //size is unknown
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = jarInputStream.read(buf)) > 0) {
                    baos.write(buf, 0, n);
                }
                bytes = baos.toByteArray();
            }
            cacheClass(url, manifest, name, bytes, entry.getCertificates());
        }
        jarInputStream.close();
    }

    private String getClassFileName(String className) {
        return className.replace(".", "/") + ".class";
    }

    private class CachedClass {
        private URL url;
        private Manifest manifest;
        private String name;
        private byte[] bytes;
        private Certificate[] certificates;

        private CachedClass(URL url, Manifest manifest, String name, byte[] bytes, Certificate[] certificates) {
            this.url = url;
            this.manifest = manifest;
            this.name = name;
            this.bytes = bytes;
            this.certificates = certificates;
        }

        private String getResourceName() {
            return getClassFileName(name);
        }

        private Class<?> define() {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    public Class<?> run() {
                        int i = name.lastIndexOf('.');
                        if (i != -1) {
                            String pkgname = name.substring(0, i);
                            Package pkg = getPackage(pkgname);
                            if (pkg == null) {
                                if (manifest != null) {
                                    definePackage(pkgname, manifest, url);
                                } else {
                                    definePackage(pkgname, null, null, null, null, null, null, null);
                                }
                            }
                        }
                        CodeSource cs = new CodeSource(url, certificates);
                        //sout("Define class " + name);
                        Class<?> c = defineClass(name, bytes, 0, bytes.length, cs);
                        //sout("Defined " + name);
                        return c;
                    }
                }, acc);
            } catch (PrivilegedActionException pae) {
                pae.printStackTrace();
                throw new IllegalStateException(pae.getException());
            }
        }
    }
    
    private void sout(String msg) {
        //System.out.println("@" + name + "@ " + msg);
    }
}
