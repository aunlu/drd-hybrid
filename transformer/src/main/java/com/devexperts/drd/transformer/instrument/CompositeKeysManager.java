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

import com.devexperts.drd.bootstrap.AbstractWeakDisposable;
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.ICompositeKeysManager;
import com.devexperts.drd.transformer.config.hb.HBManager;
import com.devexperts.drd.transformer.config.hb.HBVertex;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeKeysManager implements ICompositeKeysManager {
    private static final String PREFIX = Constants.COMPOSITE_KEY_PREFIX + ".CompositeKey";
    private final Map<String, byte[]> compositeKeysByName = new HashMap<String, byte[]>();
    private final Map<String, byte[]> compositeKeySamplesByName = new HashMap<String, byte[]>();
    private final Map<String, CompositeKeyDescriptor> descriptors = new HashMap<String, CompositeKeyDescriptor>();
    private final String[] classNamesByVertexId;
    private final AbstractWeakDisposable[] samplesByHbContractId;

    public static CompositeKeysManager getInstance() {
        return CompositeKeysManagerHolder.INSTANCE;
    }

    public byte[] getCompositeKeyBytesByName(String name) {
        if (!name.endsWith(CompositeKeyGenerator.SAMPLE_SUFFIX)) {
            byte[] b = compositeKeysByName.get(name);
            if (b != null) {
                return b;
            }
            CompositeKeyDescriptor descriptor = descriptors.get(name);
            if (descriptor == null) {
                throw new IllegalStateException("No composite key with name " + name);
            }
            b = new CompositeKeyGenerator(descriptor.types, name.replace(".", "/")).generateCompositeKeyBytes();
            compositeKeysByName.put(name, b);
            return b;
        } else {
            byte[] b = compositeKeySamplesByName.get(name);
            if (b != null) {
                return b;
            }
            final String compositeKeyName = name.substring(0, name.length() - CompositeKeyGenerator.SAMPLE_SUFFIX.length());
            CompositeKeyDescriptor descriptor = descriptors.get(compositeKeyName);
            if (descriptor == null) {
                throw new IllegalStateException("No composite key sample with name " + compositeKeyName);
            }
            b = new CompositeKeyGenerator(descriptor.types, compositeKeyName.replace(".", "/")).generateCompositeKeySampleBytes();
            compositeKeySamplesByName.put(name, b);
            return b;
        }
    }

    private CompositeKeysManager(HBVertex[] vertices) {
        int nextId = 0;
        final Map<CompositeKeyDescriptor, String> namesByDescriptor = new HashMap<CompositeKeyDescriptor, String>();
        classNamesByVertexId = new String[HBVertex.idGenerator];
        samplesByHbContractId = new AbstractWeakDisposable[HBVertex.idGenerator];
        for (HBVertex vertex : vertices) {
            final CompositeKeyDescriptor descriptor = getCompositeKeyDescriptor(vertex);
            String className = namesByDescriptor.get(descriptor);
            if (className == null) {
                className = PREFIX + nextId++;
                descriptors.put(className, descriptor);
                className += CompositeKeyGenerator.SAMPLE_SUFFIX;
                namesByDescriptor.put(descriptor, className);
                DRDLogger.log(className + " <---> " + descriptor);
            }
            classNamesByVertexId[vertex.getId()] = className;
        }
        DRDLogger.log(nextId + " composite keys generated.");
    }

    public void cacheSamples() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final Map<String, AbstractWeakDisposable> samplesByName = new HashMap<String, AbstractWeakDisposable>();
        for (int i = 0; i < classNamesByVertexId.length; i++) {
            String className = classNamesByVertexId[i];
            AbstractWeakDisposable sample = samplesByName.get(className);
            if (sample == null) {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                Class c = systemClassLoader.loadClass(className);
                DRDLogger.log(className + " loaded @ " + systemClassLoader);
                sample = (AbstractWeakDisposable) c.newInstance();
                samplesByName.put(className, sample);
            }
            samplesByHbContractId[i] = sample.newInstance();
        }
        DRDLogger.log(samplesByName.size() + " samples generated");
    }

    private CompositeKeyDescriptor getCompositeKeyDescriptor(HBVertex vertex) {
        final List<Integer> indices = vertex.getArgsIndices();
        Type[] types = new Type[indices.size()];
        Type[] args = Type.getArgumentTypes(vertex.getDescriptor());
        for (int i = 0; i < types.length; i++) {
            int index = indices.get(i);
            if (index == -1) {
                types[i] = Type.getObjectType(vertex.getOwner());
            } else {
                types[i] = args[index];
            }
        }
        return new CompositeKeyDescriptor(types);
    }

    public AbstractWeakDisposable getNewCompositeKeySample(int hbContractId) {
        return samplesByHbContractId[hbContractId].newInstance();
    }

    public int size() {
        return samplesByHbContractId.length;
    }

    private static class CompositeKeyDescriptor {
        private final Type[] types;

        private CompositeKeyDescriptor(Type[] types) {
            this.types = types;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (Type type : types) {
                sb.append(type);
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CompositeKeyDescriptor that = (CompositeKeyDescriptor) o;

            if (!Arrays.equals(types, that.types)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return types != null ? Arrays.hashCode(types) : 0;
        }
    }

    /**
     * Lazy initialize instance of {@link CompositeKeysManager}
     */
    private static class CompositeKeysManagerHolder {
        private static final CompositeKeysManager INSTANCE = new CompositeKeysManager(HBManager.getInstance().getVertices());
    }
}
