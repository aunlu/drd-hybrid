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
import com.devexperts.drd.bootstrap.DRDProperties;
import com.devexperts.drd.bootstrap.stats.Counters;
import com.devexperts.drd.bootstrap.stats.Processing;
import com.devexperts.drd.bootstrap.stats.Statistics;
import com.devexperts.drd.transformer.config.RaceDetectionType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;

@SuppressWarnings({"UnusedDeclaration"})
public class InstrumentationUtils {
    public static final int MAJOR_VERSION_MASK = 0xffff;
    public static final int MIN_CLASS_VERSION = Opcodes.V1_5; // needed to support ldc <class> and avoid "Illegal type in constant pool"

    private static final Type COUNTERS_ENUM_TYPE = Type.getType(Counters.class);
    private static final Type PROCESSING_ENUM_TYPE = Type.getType(Processing.class);
    private static final Method INCREMENT_METHOD = new Method("increment", Constants.VOID_METHOD_DESCRIPTOR);
    private static final Type STATISTICS_TYPE = Type.getType(Statistics.class);
    private static final Method NANO_TIME_METHOD = new Method("nanoTime", Type.LONG_TYPE, Constants.EMPTY_TYPE_ARRAY);
    public static final Method GET_STATISTICS_METHOD = new Method("getStatistics", STATISTICS_TYPE, Constants.EMPTY_TYPE_ARRAY);
    public static final Method GET_INTERCEPTOR_METHOD = new Method("getInterceptor", Constants.DRDInterceptorType, Constants.EMPTY_TYPE_ARRAY);
    public static final Method GET_REGISTRY_METHOD = new Method("getRegistry", Constants.DRDRegistryType, Constants.EMPTY_TYPE_ARRAY);
    public static final Method GET_DATA_PROVIDER_METHOD = new Method("getDataProvider", Constants.DRDDataProviderType, Constants.EMPTY_TYPE_ARRAY);
    private static final Method GET_COMPOSITE_KEY_BYTES_METHOD = new Method("getCompositeKeyBytes",
            Constants.BYTE_ARRAY_TYPE, new Type[]{Constants.STRING_TYPE});
    private static final Method PRINTLN_METHOD = new Method("println", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE});
    private static final Type PRINT_STREAM_TYPE = Type.getType(PrintStream.class);
    public static final Method STATISTICS_INCREMENT_METHOD = new Method("increment", Type.VOID_TYPE,
            new Type[]{COUNTERS_ENUM_TYPE, PROCESSING_ENUM_TYPE, Constants.STRING_TYPE});
    public static final Method STATISTICS_COUNT_FIELD_ACCESS_METHOD = new Method("countFieldAccess", Type.VOID_TYPE,
            new Type[]{Type.INT_TYPE});

    public static final Method CREATE_NEW_DATA_CLOCK = new Method("createNewDataClock", Constants.IDATACLOCK_TYPE, new Type[]{INT_TYPE});
    public static final Method GET_WEAK_DISPOSABLE_SAMPLE = new Method("getWeakDisposableSample", Constants.ABSTRACT_WEAK_DISPOSABLE_TYPE, new Type[]{INT_TYPE});
    public static final Method MATCHES_TO_HB_VERTEX = new Method("matchesToHBVertex", Type.BOOLEAN_TYPE, new Type[]{Constants.CLASS_TYPE, Constants.STRING_TYPE, Type.INT_TYPE});

    private InstrumentationUtils() {
    }

    public static int getComplementOpcode(int opcode) {
        switch (opcode) {
            case Opcodes.GETFIELD:
                return Opcodes.PUTFIELD;
            case Opcodes.PUTFIELD:
                return Opcodes.GETFIELD;
            case Opcodes.GETSTATIC:
                return Opcodes.PUTSTATIC;
            case Opcodes.PUTSTATIC:
                return Opcodes.GETSTATIC;
            default:
                throw new IllegalArgumentException("Unexpected opcode : " + opcode +
                        ". Should be GETFIELD|PUTFIELD|GETSTATIC|PUTSTATIC");
        }
    }

    /**
     * Returns instruction depending on top and prev computational types to duplicate top stack value at top-2 place (i.e. under prev stack value).
     * For example, if top is double (computational type 2) and prev is object (computational type 1), resulting instruction would be DUP2_X1
     * <br/>
     * <br/>
     * <b>Operand Stack:</b> <i>..., prev, top => ..., top, prev, top</i>
     *
     * @param prev type of top-1 stack value
     * @param top  type of top stack value
     * @return instruction that should be called to duplicate top stack value at under prev stack value.
     */
    public static int dupX1(Type prev, Type top) {
        boolean topCategory2 = isCategory2(top);
        boolean prevCategory2 = isCategory2(prev);
        return topCategory2 ? prevCategory2 ? Opcodes.DUP2_X2 : Opcodes.DUP2_X1 : prevCategory2 ? Opcodes.DUP_X2 : Opcodes.DUP_X1;
    }

    public static int pop(Type top) {
        return isCategory2(top) ? Opcodes.POP2 : Opcodes.POP;
    }

    public static int dupSmthOver1(Type top) {
        return isCategory2(top) ? Opcodes.DUP2_X1 : Opcodes.DUP_X1;
    }

    public static int dup(Type top) {
        return isCategory2(top) ? Opcodes.DUP2 : Opcodes.DUP;
    }

    private static boolean isCategory2(Type top) {
        return top.getSize() == 2;
    }

    /**
     * Inserts to JVM stack instructions for {@code System.out.println(s);}
     *
     * @param s  string to println
     * @param mv method visitor, that right now is writing some code
     */
    public static void sout(String s, GeneratorAdapter mv) {
        //STACK: <...>
        mv.getStatic(Constants.SYSTEM_TYPE, "out", PRINT_STREAM_TYPE);
        mv.push(s);
        mv.invokeVirtual(PRINT_STREAM_TYPE, PRINTLN_METHOD);
        //STACK: <...>
    }

    /**
     * Inserts to JVM stack instructions for {@code System.out.println(s);} , where s is string on the top of the stack,
     * removing s from the top of the stack
     * <br/>
     * <b>Operand Stack:</b> <i>..., string => ...</i><br/>
     * objref should be instance of String.
     *
     * @param mv method visitor, that right now is writing some code
     */
    public static void sout(GeneratorAdapter mv) {
        //STACK: <...> <str>
        mv.getStatic(Constants.SYSTEM_TYPE, "out", PRINT_STREAM_TYPE);
        mv.swap();
        mv.invokeVirtual(PRINT_STREAM_TYPE, PRINTLN_METHOD);
        //STACK: <...>
    }

    public static void debug(GeneratorAdapter mv) {
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("debug", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void debug(String msg, GeneratorAdapter mv) {
        mv.push(msg);
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("debug", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void log(GeneratorAdapter mv) {
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("log", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void log(String msg, GeneratorAdapter mv) {
        mv.push(msg);
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("log", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void error(GeneratorAdapter mv) {
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("error", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void error(String msg, GeneratorAdapter mv) {
        mv.push(msg);
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("error", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void errorWithStackTrace(String msg, GeneratorAdapter mv) {
        mv.push(msg);
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("errorWithStackTrace", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void errorWithStackTrace(GeneratorAdapter mv) {
        mv.invokeStatic(Type.getType(DRDLogger.class), new Method("errorWithStackTrace", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    /**
     * Pushes {@link com.devexperts.drd.bootstrap.DRDInterceptor} instance to the top of the call stack
     *
     * @param mv method visitor, that currently visits method
     */
    public static void pushInterceptor(GeneratorAdapter mv) {
        mv.invokeStatic(Constants.DRDEntryPointType, GET_INTERCEPTOR_METHOD);
    }

    /**
     * Pushes {@link com.devexperts.drd.bootstrap.DataProvider} instance to the top of the call stack
     *
     * @param mv method visitor, that currently visits method
     */
    public static void pushDataProvider(GeneratorAdapter mv) {
        mv.invokeStatic(Constants.DRDEntryPointType, GET_DATA_PROVIDER_METHOD);
    }

    /**
     * STACK: composite_key_name ---> byte[]
     *
     * @param mv method visitor, that currently visits method
     */
    public static void getCompositeKeyBytes(GeneratorAdapter mv) {
        mv.invokeStatic(Constants.DRDEntryPointType, GET_COMPOSITE_KEY_BYTES_METHOD);
    }

    public static boolean areInputArgsSame(final String methodDescriptor1, final String methodDescriptor2) {
        int index = methodDescriptor1.indexOf(")");
        return methodDescriptor2.indexOf(")") == index &&
                methodDescriptor1.substring(0, index).equals(methodDescriptor2.substring(0, index));
    }

    private void pushDefaultValue(Type type, MethodVisitor mv) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(ICONST_0);
                break;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                break;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                break;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitInsn(ACONST_NULL);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized type sort for " + type);
        }
    }

    public static boolean isPrimitive(Type type) {
        return (type == BOOLEAN_TYPE) || (type == Type.BYTE_TYPE) || (type == Type.CHAR_TYPE) ||
                (type == Type.SHORT_TYPE) || (type == Type.INT_TYPE) || (type == Type.LONG_TYPE) ||
                (type == Type.FLOAT_TYPE) || (type == Type.DOUBLE_TYPE);
    }

    public static String fieldOpcodeToString(int opcode) {
        switch (opcode) {
            case Opcodes.GETFIELD:
                return "GETFIELD";
            case Opcodes.GETSTATIC:
                return "GETSTATIC";
            case Opcodes.PUTSTATIC:
                return "PUTSTATIC";
            case Opcodes.PUTFIELD:
                return "PUTFIELD";
            default:
                throw new IllegalArgumentException("Unexpected opcode : " + opcode);
        }
    }

    public static boolean isWrite(int opcode) {
        switch (opcode) {
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
                return false;
            case Opcodes.PUTSTATIC:
            case Opcodes.PUTFIELD:
                return true;
            default:
                throw new IllegalArgumentException("Unexpected opcode : " + opcode);
        }
    }

    public static String toString(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return "Boolean";
            case Type.BYTE:
                return "Byte";
            case Type.SHORT:
                return "Short";
            case Type.CHAR:
                return "Char";
            case Type.INT:
                return "Int";
            case Type.LONG:
                return "Long";
            case Type.FLOAT:
                return "Float";
            case Type.DOUBLE:
                return "Double";
            default:
                return "Object";
        }
    }

    public static <K, V> void put(K key, V value, Map<K, List<V>> map) {
        final List<V> values = map.get(key);
        if (values == null) {
            final List<V> newValues = new ArrayList<V>();
            newValues.add(value);
            map.put(key, newValues);
        } else {
            values.add(value);
        }
    }

    public static int[] storeArgsToLocals(GeneratorAdapter mv, String methodDesc) {
        Type[] args = Type.getArgumentTypes(methodDesc);
        int[] locals = new int[args.length];
        for (int i = args.length - 1; i >= 0; i--) {
            locals[i] = mv.newLocal(args[i]);
            mv.storeLocal(locals[i], args[i]);
        }
        return locals;
    }

    public static void loadArgsFromLocals(GeneratorAdapter mv, int[] locals) {
        for (int i : locals) {
            mv.loadLocal(i);
        }
    }

    public static void invoke(GeneratorAdapter mv, InterceptorMethod method) {
        mv.invokeInterface(Constants.DRDInterceptorType, method.method);
    }

    /**
     * STACK: string --> boolean
     */
    public static void startsWith(GeneratorAdapter mv, String substring) {
        mv.push(substring);
        mv.invokeVirtual(Constants.STRING_TYPE, new Method("startsWith", BOOLEAN_TYPE, new Type[]{Constants.STRING_TYPE}));
    }

    public static void track(Counters counter, Processing p, String description, GeneratorAdapter mv) {
        if (DRDProperties.profilingEnabled) {
            mv.invokeStatic(Constants.DRDEntryPointType, GET_STATISTICS_METHOD);
            mv.getStatic(COUNTERS_ENUM_TYPE, counter.toString().toUpperCase(), COUNTERS_ENUM_TYPE);
            mv.getStatic(PROCESSING_ENUM_TYPE, p.toString().toUpperCase(), PROCESSING_ENUM_TYPE);
            mv.push(description);
            mv.invokeInterface(STATISTICS_TYPE, STATISTICS_INCREMENT_METHOD);
        }
    }

    public static void trackFieldAccess(String description, boolean write, GeneratorAdapter mv) {
        if (DRDProperties.profilingEnabled) {
            mv.invokeStatic(Constants.DRDEntryPointType, GET_STATISTICS_METHOD);
            mv.push(description);
            mv.push(write);
            mv.invokeInterface(STATISTICS_TYPE, new Method("trackFieldAccess", Type.VOID_TYPE,
                    new Type[]{Constants.STRING_TYPE, Type.BOOLEAN_TYPE}));
        }
    }

    public static void trackFieldAccess(RaceDetectionType type, GeneratorAdapter mv) {
        if (DRDProperties.profilingEnabled) {
            mv.invokeStatic(Constants.DRDEntryPointType, GET_STATISTICS_METHOD);
            mv.push(type.ordinal());
            mv.invokeInterface(STATISTICS_TYPE, STATISTICS_COUNT_FIELD_ACCESS_METHOD);
        }
    }

    public static void lockedHard(String name, GeneratorAdapter mv) {
        if (DRDProperties.profilingEnabled) {
            mv.invokeStatic(Constants.DRDEntryPointType, GET_STATISTICS_METHOD);
            mv.push(name);
            mv.invokeInterface(STATISTICS_TYPE, new Method("lockedHard", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
        }
    }

    public static void lockedSoft(String name, GeneratorAdapter mv) {
        if (DRDProperties.profilingEnabled) {
            mv.invokeStatic(Constants.DRDEntryPointType, GET_STATISTICS_METHOD);
            mv.push(name);
            mv.invokeInterface(STATISTICS_TYPE, new Method("lockedSoft", Type.VOID_TYPE, new Type[]{Constants.STRING_TYPE}));
        }
    }

    public static void writeTransformedFile(String className, byte[] modifiedClassFileBuffer, String suffix) {
        String transformedFilesDir = DRDProperties.getTransformedFilesDir();
        if (transformedFilesDir == null) {
            return;
        }
        final File file = new File(transformedFilesDir + suffix, className + Constants.CLASS_SUFFIX);
        final File parentFile = file.getParentFile();
        parentFile.mkdirs();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(modifiedClassFileBuffer);
        } catch (FileNotFoundException e) {
            DRDLogger.error(suffix + " failed to write transformed file.", e);
        } catch (IOException e) {
            DRDLogger.error(suffix + " failed to write transformed file.", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    //skip
                }
            }
        }
    }
}
