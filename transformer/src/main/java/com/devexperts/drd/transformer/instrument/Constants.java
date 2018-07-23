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

import com.devexperts.drd.bootstrap.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class Constants {
    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];
    public static final String CLASS_SUFFIX = ".class";
    public static final String DRD_SYNTHETIC_CLASSES_PREFIX = "drd";
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final String VC_SUFFIX = "$vc";
    public static final String INIT_METHOD = "<init>";
    public static final String CLINIT_METHOD = "<clinit>";
    public static final String VOID_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, EMPTY_TYPE_ARRAY);
    public static final Type IDATACLOCK_TYPE = Type.getType(IDataClock.class);
    public static final Type ISYNCCLOCK_TYPE = Type.getType(ISyncClock.class);
    public static final String IDATACLOCK_DESC = IDATACLOCK_TYPE.getDescriptor();
    public static final Type BYTE_ARRAY_TYPE = Type.getType(byte[].class);
    public static final Type THREAD_TYPE = Type.getType(Thread.class);
    public static final Type STRING_TYPE = Type.getType(String.class);
    public static final Type CLASS_TYPE = Type.getType(Class.class);
    public static final Type DRDEntryPointType = Type.getType(DRDEntryPoint.class);
    public static final Type DRDInterceptorType = Type.getType(DRDInterceptor.class);
    public static final Type DRDRegistryType = Type.getType(DRDRegistry.class);
    public static final Type DRDDataProviderType = Type.getType(DataProvider.class);
    public static final Type ClockedType = Type.getType(Clocked.class);
    public static final Type ABSTRACT_WEAK_DISPOSABLE_TYPE = Type.getType(AbstractWeakDisposable.class);
    public static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);
    public static final Type SYSTEM_TYPE = Type.getType(System.class);
    public static final String COMPOSITE_KEY_PREFIX = "com.devexperts.drd.bootstrap.$composite";
    public static final String SYNTHETIC_DRD_PREFIX = "drd";
    public static final Type DRD_AGENT_BOOTSTRAP_TYPE = Type.getType(DRDAgentBootstrap.class);
    //TODO all calls - through registry!
    public static final Method DRDInterceptorStart = new Method("beforeStart", Type.VOID_TYPE, new Type[]{THREAD_TYPE});
    public static final Method DRDInterceptorDie = new Method("beforeDying", Type.VOID_TYPE, EMPTY_TYPE_ARRAY);
    public static final Method LOAD_CLASS_METHOD = new Method("loadClass", CLASS_TYPE, new Type[]{STRING_TYPE});
    public static final Method LOAD_CLASS_METHOD_RESOLVE = new Method("loadClass", CLASS_TYPE, new Type[]{STRING_TYPE, Type.BOOLEAN_TYPE});
}