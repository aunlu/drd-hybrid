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

package com.devexperts.drd.agent.core;

import com.devexperts.drd.agent.*;
import com.devexperts.drd.agent.clock.*;
import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.agent.util.ConcurrentWeakHashMap;
import com.devexperts.drd.agent.util.LongHashMap;
import com.devexperts.drd.agent.util.LongMap;
import com.devexperts.drd.bootstrap.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClocksStorage {
    private static final long STATS_GATHER_DELAY = 1000 * 30L; //30 s
    private static final long STATS_GATHER_FREQUENCY = 1000L * 60L; //once in 1 min
    private static final long NANO_TO_MILLIS = 1000000L;

    private static final VectorClocksManager synClocksManager = new VectorClocksManager(
            new ConcurrentWeakHashMap<Object, SyncClock>(IdentityComparison.INSTANCE));
    private static final ManualSyncClockManager manualSynClocksManager = new ManualSyncClockManager();
    private static final ManualSyncClockManager volatileClocksManager = new ManualSyncClockManager();
    private static final DataClockManager foreignClocksManager = new DataClockManager(
            new ConcurrentWeakHashMap<Object, DataClock>(IdentityComparison.INSTANCE));
    /**
     * Map to store thread's vector clocks for specific needs:
     * <ol>
     * <li>If thread forks another one with tid == child_tid, it stores his(parent's) vc copy at child_tid's id.
     * Then child, from it's "start" method gets parents clock from this map and consider them as it's initial clock.</li>
     * <li>If thread dies, he puts his clock into this map. Then, if some other thread
     * returns from join() on died thread, he may pick up clock of dead thread from this map.</li>
     * </ol>
     */
    private static final LongMap<SyncClock> threadBoundaryClocks = new LongHashMap<SyncClock>();
    private static AtomicInteger threadClocksCount = new AtomicInteger(0);
    private static long lastStatsGatherTime = 0;

    //real thread's vector clocks
    private static ThreadLocal<ThreadVectorClock> threadClocks = new ThreadLocal<ThreadVectorClock>() {
        protected ThreadVectorClock initialValue() {
            final ThreadVectorClock tvc = new ThreadVectorClock();
            final Thread thread = Thread.currentThread();
            final long tid = thread.getId();
            for (long l : drdThreads) {
                if (l == tid) {
                    DRDLogger.log("Do not create tvc for drd thread id = " + l);
                    return null;
                }
            }
            loadIntoThreadClock(tvc, tid);
            if (DRDProperties.soutEnabled) {
                DRDLogger.log("Thread clock for " + thread + " id = " + tid +
                        " created : " + tvc + " Total thread clocks count : " + threadClocksCount.incrementAndGet());
            }
            return tvc;
        }
    };

    private static volatile long[] drdThreads = new long[0];

    private static String clearManualSynClocksManager(ManualSyncClockManager manager) {
        long time = System.nanoTime();
        int disposedCounter = 0;
        Iterator<Map.Entry<WeakDisposable, SyncClock>> it = manager.clocks.entrySet().iterator();
        for (; it.hasNext(); ) {
            Map.Entry<WeakDisposable, SyncClock> next = it.next();
            if (next.getKey().canDelete()) {
                it.remove();
                disposedCounter++;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(manager.clocks.size()).append(" (");
        final long created = manager.newClockCounter.estimateGetAndReset();
        sb.append(" + ").append(created).append(" - ").append(disposedCounter).append(" = ").append(created - disposedCounter);
        sb.append(".");
        sb.append(" Disposal took ").append((System.nanoTime() - time) / NANO_TO_MILLIS).append(" ms).");
        return sb.toString();
    }

    static {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private int id = 0;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("DRD-stats-collector " + id++);
                synchronized (ClocksStorage.class) {
                    final int length = drdThreads.length;
                    long[] drdThreadsCopy = new long[length + 1];
                    System.arraycopy(drdThreads, 0, drdThreadsCopy, 0, length);
                    final long tid = thread.getId();
                    drdThreadsCopy[length] = tid;
                    drdThreads = drdThreadsCopy;
                    DRDLogger.log("1 more DRD thread : " + tid);
                }
                return thread;
            }
        });
        final Runnable r = new Runnable() {
            public void run() {
                Stats.gather();
            }
        };
        executor.scheduleAtFixedRate(r, STATS_GATHER_DELAY, STATS_GATHER_FREQUENCY, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                r.run();
            }
        }));
    }

    /**
     * This method should be called by thread every name it creates new Thread with tid specified
     *
     * @param tid tid of child thread
     */
    public static void onFork(long tid) {
        if (DRDProperties.soutEnabled) {
            DRDLogger.log("Thread " + Thread.currentThread().getId() + " forks thread " + tid);
        }
        storeCurrentThreadClock(tid);
    }

    public static void onDie() {
        ThreadVectorClock tvc = getThreadClock();
        if (tvc != null) {
            Generations.threadDied(tvc.tid);
            final Thread thread = Thread.currentThread();
            storeCurrentThreadClock(thread.getId());
        }
    }

    public static void afterJoin(long tid) {
        final ThreadVectorClock tvc = getThreadClock();
        if (tvc != null) {
            loadIntoThreadClock(tvc, tid);
        }
    }

    private static void storeCurrentThreadClock(long targetTid) {
        final ThreadVectorClock currentClock = getThreadClock();
        if (currentClock != null) {
            currentClock.tick();
            synchronized (threadBoundaryClocks) {
                threadBoundaryClocks.put(targetTid, new SyncClock(currentClock));
            }
            if (DRDProperties.soutEnabled) {
                DRDLogger.log("TC " + currentClock + " stored by tid = " + targetTid);
            }
        }
    }

    private static void loadIntoThreadClock(ThreadVectorClock currentClock, long tid) {
        synchronized (threadBoundaryClocks) {
            final SyncClock clock = threadBoundaryClocks.get(tid);
            StringBuilder sb = new StringBuilder();
            if (clock != null) {
                sb.append("Parent clock ").append(clock).append(" loaded to clock ").append(currentClock);
                clock.loadTo(currentClock);
                sb.append(" ---> ").append(currentClock);
            } else {
                sb.append("Parent clock not found for tid = ").append(Thread.currentThread().getId());
            }
            if (DRDProperties.soutEnabled) {
                DRDLogger.log(sb.toString());
            }
        }
    }

    public static ThreadVectorClock getThreadClock() {
        return threadClocks.get();
    }

    public static SyncClock getSynClock(Object ref) {
        return synClocksManager.getClock(-1, ref);
    }

    public static SyncClock getManualSynClock(WeakDisposable ref) {
        return manualSynClocksManager.getClock(ref);
    }

    public static SyncClock getOrCreateManualSynClock(WeakDisposable ref) {
        return manualSynClocksManager.getClockSupposingTheyAreAbsent(ref);
    }

    public static SyncClock getOrCreateVolatilesClock(WeakDisposable ref) {
        return volatileClocksManager.getClockSupposingTheyAreAbsent(ref);
    }

    public static SyncClock getVolatilesClock(WeakDisposable ref) {
        return volatileClocksManager.getClock(ref);
    }

    public static DataClock getForeignClock(int ownerId, Object ref) {
        return foreignClocksManager.getClock(ownerId, ref);
    }

    private static class ManualSyncClockManager {
        private final ConcurrentHashMap<WeakDisposable, SyncClock> clocks = new ConcurrentHashMap<WeakDisposable, SyncClock>();
        private final Counter newClockCounter = new Counter();
        final Counter profiler = new Counter();

        public SyncClock getClock(WeakDisposable o) {
            profiler.increment();
            return clocks.get(o);
        }

        public SyncClock getClockSupposingTheyAreAbsent(WeakDisposable o) {
            SyncClock clock = new SyncClock();
            SyncClock old = clocks.putIfAbsent(o, clock);
            if (old != null) {
                clock = old;
            } else newClockCounter.increment();
            return clock;
        }
    }

    private static abstract class AbstractClocksManager<T extends VectorClock> {
        final Counter newClockForNothingProfiler = new Counter();
        final Counter newClockProfiler = new Counter();
        final ConcurrentWeakHashMap<Object, T> clocks;
        final Counter profiler = new Counter();

        private AbstractClocksManager(ConcurrentWeakHashMap<Object, T> map) {
            if (map.size() > 0) throw new IllegalStateException("map should be empty");
            this.clocks = map;
        }

        public T getClock(int ownerId, Object ref) {
            profiler.increment();
            T clock = clocks.get(ref);
            if (clock == null) {
                clock = createNewClock(ownerId);
                T old = clocks.putIfAbsent(ref, clock);
                if (old != null) {
                    newClockForNothingProfiler.increment();
                    clock = old;
                } else newClockProfiler.increment();
            }
            return clock;
        }

        public int getClocksCount() {
            return clocks.size();
        }

        protected abstract T createNewClock(int ownerId);
    }

    private static class VectorClocksManager extends AbstractClocksManager<SyncClock> {
        private VectorClocksManager(ConcurrentWeakHashMap<Object, SyncClock> map) {
            super(map);
        }

        @Override
        protected SyncClock createNewClock(int ownerId) {
            return new SyncClock();
        }
    }

    private static class DataClockManager extends AbstractClocksManager<DataClock> {
        private DataClockManager(ConcurrentWeakHashMap<Object, DataClock> map) {
            super(map);
        }

        @Override
        protected DataClock createNewClock(int ownerId) {
            return new DataClock(ownerId);
        }
    }

    private static class Stats {
        public static void gather() {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\n\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                long time = System.nanoTime();
                if (lastStatsGatherTime != 0) {
                    sb.append("\nLast time stats were gathered ").append((time - lastStatsGatherTime) / NANO_TO_MILLIS).append(" ms ago.");
                }
                lastStatsGatherTime = time;
                sb.append("\n").append(ManagementFactory.getThreadMXBean().getThreadCount()).append(" active threads.");
                final MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                sb.append("\nMemory usage: ").append(heapMemoryUsage.toString()).append(".");
                long realSize = 0;
                long reservedSize = 0;
                int deadSize = 0;
                Map<Integer, MutableInteger> realBySize = new TreeMap<Integer, MutableInteger>();
                Map<Integer, MutableInteger> reservedBySize = new TreeMap<Integer, MutableInteger>();

                sb.append("\nContract clocks: ");
                sb.append(clearManualSynClocksManager(manualSynClocksManager));
                sb.append("\n\tHits : ").append(manualSynClocksManager.profiler.estimateGetAndReset()).append(".");
                for (VectorClock clock : manualSynClocksManager.clocks.values()) {
                    int size = clock.getRealSizeUnprotected();
                    MutableInteger mi = realBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        realBySize.put(size, mi);
                    }
                    mi.value++;
                    realSize += size;
                    size = clock.getReservedSizeUnprotected();
                    mi = reservedBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        reservedBySize.put(size, mi);
                    }
                    mi.value++;
                    reservedSize += size;
                    deadSize += clock.getDeadClockSize();
                }
                sb.append("\n\tApprox total reserved size: ").append(reservedSize).append("; real size : ").append(realSize);
                sb.append(". Dead threads clocks size: ").append(deadSize).append(".");

                realSize = 0;
                reservedSize = 0;
                deadSize = 0;
                realBySize.clear();
                reservedBySize.clear();

                sb.append("\nVolatile clocks: ");
                sb.append(clearManualSynClocksManager(volatileClocksManager));
                sb.append("\n\tHits : ").append(volatileClocksManager.profiler.estimateGetAndReset()).append(".");

                for (VectorClock clock : volatileClocksManager.clocks.values()) {
                    int size = clock.getRealSizeUnprotected();
                    MutableInteger mi = realBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        realBySize.put(size, mi);
                    }
                    mi.value++;
                    realSize += size;
                    size = clock.getReservedSizeUnprotected();
                    mi = reservedBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        reservedBySize.put(size, mi);
                    }
                    mi.value++;
                    reservedSize += size;
                    deadSize += clock.getDeadClockSize();
                }
                sb.append("\n\tApprox total reserved size: ").append(reservedSize).append("; real size : ").append(realSize);
                sb.append(". Dead threads clocks size: ").append(deadSize).append(".");

                realSize = 0;
                reservedSize = 0;
                deadSize = 0;

                sb.append("\nSyn clocks: ").append(synClocksManager.getClocksCount());
                long created = synClocksManager.newClockProfiler.estimateGetAndReset();
                int disposed = synClocksManager.clocks.getWeakClearedCount();
                sb.append(" ( + ").append(created).append(" - ").append(disposed).append(" = ").append(created - disposed);
                sb.append("). New clock for nothing : ").append(synClocksManager.newClockForNothingProfiler.estimateGetAndReset()).append(".");
                sb.append("\n\tHits : ").append(synClocksManager.profiler.estimateGetAndReset()).append(".");

                for (VectorClock clock : synClocksManager.clocks.values()) {
                    int size = clock.getRealSizeUnprotected();
                    MutableInteger mi = realBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        realBySize.put(size, mi);
                    }
                    mi.value++;
                    realSize += size;
                    size = clock.getReservedSizeUnprotected();
                    mi = reservedBySize.get(size);
                    if (mi == null) {
                        mi = new MutableInteger();
                        reservedBySize.put(size, mi);
                    }
                    mi.value++;
                    reservedSize += size;
                    deadSize += clock.getDeadClockSize();
                }
                sb.append("\n\tApprox total reserved size : ").append(reservedSize).append("; real size : ").append(realSize);
                sb.append(". Dead threads clocks size : ").append(deadSize).append(".");
                sb.append("\nForeign clocks: ").append(foreignClocksManager.getClocksCount());
                created = foreignClocksManager.newClockProfiler.estimateGetAndReset();
                disposed = foreignClocksManager.clocks.getWeakClearedCount();
                sb.append(" ( + ").append(created).append(" - ").append(disposed).append(" = ").append(created - disposed);
                sb.append("). New clock for nothing : ").append(foreignClocksManager.newClockForNothingProfiler.estimateGetAndReset()).append(".");

                sb.append("\n\tHits : ").append(foreignClocksManager.profiler.estimateGetAndReset());

                for (VectorClock clock : foreignClocksManager.clocks.values()) {
                    int size = clock.getRealSizeUnprotected();
                    realSize += size;
                    size = clock.getReservedSizeUnprotected();
                    reservedSize += size;
                    deadSize += clock.getDeadClockSize();
                }
                sb.append("\n\tApprox total reserved size: ").append(reservedSize).append("; real size : ").append(realSize);
                sb.append(". Dead threads clocks size: ").append(deadSize);

                sb.append("\n\nVector clock counters: ");
                sb.append("\n\tNew array allocations for live clocks: ").append(VectorClockUtils.liveAllocCounter.estimateGetAndReset());
                sb.append(" vs ").append(VectorClockUtils.liveNotAllocCounter.estimateGetAndReset()).append(" reusages.");
                sb.append("\n\tDead clocks during merge: ").append(VectorClockUtils.cachedCounter.estimateGetAndReset()).append(" cached, ");
                sb.append(VectorClockUtils.sameCounter.estimateGetAndReset()).append(" same, ");
                sb.append(VectorClockUtils.copyCounter.estimateGetAndReset()).append(" new.");
                sb.append("\n\tGenerations update: ").append(VectorClock.lightGenerationUpdateCounter.estimateGetAndReset()).append(" light, ");
                sb.append(VectorClock.hardGenerationUpdateCounter.estimateGetAndReset()).append(" hard (");
                sb.append(VectorClock.zeroGenCounter.estimateGetAndReset()).append(" of them from zero).");
                sb.append("\n\tVector clock merge: ")
                        .append(SyncClock.optimisedAcquireProfiler.estimateGetAndReset()).append(" optimized vs ")
                        .append(SyncClock.fullAcquireProfiler.estimateGetAndReset()).append(" full acquires; ")
                        .append(SyncClock.optimisedReleaseProfiler.estimateGetAndReset()).append(" optimized vs ")
                        .append(SyncClock.fullReleaseProfiler.estimateGetAndReset()).append(" full releases. ")
                        .append(SyncClock.twoWayProfiler.estimateGetAndReset()).append(" two-way merges. ");
                sb.append("\nDiff was calculated ").append(Generations.diffProfiler.estimateGetAndReset()).append(" times.");
                sb.append("\n\tShared reads in data clock occurred ").append(DataClock.sharedReadsCounter.estimateGetAndReset()).append(" times.");
                sb.append("\n\tVC total live resizes: ").append(VectorClock.resizeProfiler.estimateGetAndReset());
                sb.append(", total dead resizes: ").append(VectorClock.deadResizeProfiler.estimateGetAndReset()).append(".");
                sb.append("\n\tYield counter : ").append(VectorClock.yieldCounter.estimateGetAndReset()).append(".");
                sb.append(DRDEntryPoint.getStatistics().dumpAndClear());
                sb.append(DRDEntryPoint.getStatistics().dumpFieldAccesses());
                int limit = DRDProperties.dataClockHistogramLimit;
                sb.append("\n\n-----------------Data clock histogram (top ").append(limit).append("): -----------------\n");
                int counter = 0;
                List<DataClockStats> histogram = DRDEntryPoint.getRegistry().getDataClockHistogram();
                for (DataClockStats entry : histogram) {
                    if (limit-- > 0) {
                        sb.append(entry.count).append("   ").append(entry.className).append("\n");
                    }
                    counter += entry.count;
                }
                sb.append("--------------------Total: ").append(counter).append("------------------------\n\n");
                sb.append("\n\nStats gathered in ").append((System.nanoTime() - time) / NANO_TO_MILLIS).append(" ms").append(".");
                sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
                DRDLogger.log(sb.toString());
            } catch (Throwable e) {
                e.printStackTrace();
                DRDLogger.error("Error in stats gathering : ", e);
            }
        }
    }
}
