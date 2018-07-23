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

package com.devexperts.drd.agent;

import com.devexperts.drd.bootstrap.DRDEntryPoint;

public class ThreadUtils {
    private ThreadUtils() {
    }

    public static ThreadGroup getRootThreadGroup() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup root = null;
        while (group != null) {
            root = group;
            group = group.getParent();
        }
        return root;
    }

    public static Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup();
        final Thread[] allThreads = new Thread[root.activeCount()];
        root.enumerate(allThreads, true);
        return allThreads;
    }

    public static Thread getThread(long tid) {
        for (final Thread thread : getAllThreads()) {
            if (thread != null && thread.getId() == tid) return thread;
        }
        return null;
    }

    public static StackTraceElement[] removeTopDRDCalls(StackTraceElement[] stackTraceElements) {
        int bound = 0;
        for (StackTraceElement e : stackTraceElements) {
            if (e.getClassName().startsWith("com.devexperts.drd.") ||
                    e.getClassName().equals("java.lang.Thread") && e.getMethodName().equals("getStackTrace")) bound++;
            else break;
        }
        StackTraceElement[] result = new StackTraceElement[stackTraceElements.length - bound];
        System.arraycopy(stackTraceElements, bound, result, 0, result.length);
        return result;
    }

    public static String toString(StackTraceElement[] ste) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement aSte : ste) {
            sb.append("\tat ").append(aSte).append("\n");
        }
        return sb.toString();
    }

    public static String getThreadDescription(long tid) {
        return DRDEntryPoint.getRegistry().getThreadName(tid) + " (tid = " + tid + ")";
    }
}
