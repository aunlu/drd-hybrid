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

import com.devexperts.drd.bootstrap.DRDLogger;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ThreadDumper extends Thread {
    private static final Comparator<ThreadInfo> THREAD_ID_COMPARATOR = new Comparator<ThreadInfo>() {
        public int compare(ThreadInfo t1, ThreadInfo t2) {
            return t1.getThreadId() < t2.getThreadId() ? -1 : t1.getThreadId() > t2.getThreadId() ? 1 : 0;
        }
    };
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.sssZ", Locale.US);

    private final String header = "Full thread dump " + System.getProperty("java.vm.name") +
            " (" + System.getProperty("java.vm.version") + " " + System.getProperty("java.vm.info") + "):";
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final String file;
    private final long delay;
    private final long period;

    public ThreadDumper(String file, long delay, long period) {
        super("DRD-Thread-Dumper");
        this.file = file;
        this.delay = delay;
        this.period = period;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            DRDLogger.debug("ThreadDumper thread launched.");
            Thread.sleep(delay);
            // open file & make dumps
            PrintStream out = file.isEmpty() ? System.out : new PrintStream(file);
            try {
                while (!interrupted()) {
                    sleep(period);
                    makeThreadDump(out);
                }
            } finally {
                if (!file.isEmpty())
                    out.close();
                DRDLogger.debug("ThreadDumper thread terminated.");
            }
        } catch (InterruptedException e) {
            // interrupted from JVMSelfMonitoring -- quit
        } catch (Throwable t) {
            DRDLogger.error("Failed to make thread dumps", t);
        }
    }

    private void makeThreadDump(PrintStream out) {
        // get all thread infos
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(
                threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported());
        Arrays.sort(threadInfos, THREAD_ID_COMPARATOR);
        Date now = new Date();
        out.println(dateFormat.format(now) + "T" + timeFormat.format(now) + ": ThreadDumper: Writing thread dump");
        out.println(header);
        out.println();
        for (ThreadInfo ti : threadInfos) {
            String lockOwner = ti.getLockOwnerId() > 0 ? " owned by \"" + ti.getLockOwnerName() + "\" id=" + ti.getLockOwnerId() : "";
            String lockInfo = ti.getLockName() != null ? " (lock=" + ti.getLockName() + lockOwner + ")" : "";
            out.println("\"" + ti.getThreadName() + "\"" + " id=" + ti.getThreadId() + " " + ti.getThreadState() + lockInfo);
            for (StackTraceElement ste : ti.getStackTrace())
                out.println("\tat " + ste);
            out.println();
        }
        out.flush();
    }
}
