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

import com.devexperts.drd.agent.high_scale_lib.Counter;
import com.devexperts.drd.agent.high_scale_lib.NonBlockingHashMap;
import com.devexperts.drd.agent.high_scale_lib.NonBlockingHashSet;
import com.devexperts.drd.bootstrap.BlackHole;
import com.devexperts.drd.bootstrap.stats.Counters;
import com.devexperts.drd.bootstrap.stats.Processing;
import com.devexperts.drd.bootstrap.stats.Statistics;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Class that encapsulates all profiling counters. For the sake of performance counters are stored each in standalone variable, not in the map.
 */
@SuppressWarnings("UnusedDeclaration")
public class InternalStatistics implements Statistics {
    public static final int TOP = 100;
    //unroll enum map for better performance
    private final Gauge fieldAccessGauge = new Gauge();
    private final Gauge foreignCallGauge = new Gauge();
    private final Gauge syncMethodEnterGauge = new Gauge();
    private final Gauge syncMethodExitGauge = new Gauge();
    private final Gauge monEnterEnterGauge = new Gauge();
    private final Gauge monExitGauge = new Gauge();
    private final Gauge waitGauge = new Gauge();
    private final Gauge joinGauge = new Gauge();
    private final Gauge volatileGauge = new Gauge();
    private final Gauge contractSyncGauge = new Gauge();
    private final Gauge raceGauge = new Gauge();
    private final Counter detectCounter = new Counter();
    private final Counter ignoreScopeCounter = new Counter();
    private final Counter ignoreRuleCounter = new Counter();
    private final Counter ignoreConstructorCounter = new Counter();
    private final Counter ignoreFinalCounter = new Counter();
    private final Counter ignoreErrorCounter = new Counter();

    private final Set<String> lockedSoft = new NonBlockingHashSet<String>();
    private final Set<String> lockedHard = new NonBlockingHashSet<String>();
    private final Counter totalMethodsTransformed = new Counter();

    private NonBlockingHashMap<String, Gauge> fieldAccesses = new NonBlockingHashMap<String, Gauge>();

    public InternalStatistics() {
        //init internals
        increment(Counters.FIELD_ACCESS, Processing.IGNORED, "INIT_INTERNALS");
        BlackHole.BLACK_HOLE.print(getClass().getSimpleName() + " initialized.");
    }

    public void lockedSoft(String name) {
        lockedSoft.add(name);
    }

    public void lockedHard(String name) {
        lockedHard.add(name);
    }

    public void transformed() {
        totalMethodsTransformed.increment();
    }

    public void trackFieldAccess(String description, boolean write) {
        Gauge g = fieldAccesses.get(description);
        if (g == null) {
            g = new Gauge();
            Gauge old = fieldAccesses.putIfAbsent(description, g);
            if (old != null) {
                g = old;
            }
        }
        g.process(write ? Processing.PROCESSED : Processing.IGNORED);
    }

    public String dumpFieldAccesses() {
        PriorityQueue<AccessCounter> pq = new PriorityQueue<AccessCounter>(TOP);
        for (Map.Entry<String, Gauge> e : fieldAccesses.entrySet()) {
            AccessCounter ac = new AccessCounter(e.getKey(), e.getValue());
            if (pq.size() < TOP) {
                pq.add(ac);
            } else if (pq.peek().compareTo(ac) < 0) {
                pq.poll();
                pq.add(ac);
            }
        }

        StringBuilder sb = new StringBuilder("\n-----------Top " + TOP + " accessed fields: total = writes+reads\n\n");
        AccessCounter[] array = new AccessCounter[pq.size()];
        AccessCounter a;
        int i = pq.size();
        while ((a = pq.poll()) != null) {
            array[--i] = a;
        }
        for (AccessCounter ac : array) {
            sb.append(ac.total).append(" = ").append(ac.write).append("+").append(ac.read).append(" ").append(ac.description).append("\n");
        }
        return sb.toString();
    }

    public void trackForeignCall(String description, boolean write) {
    }

    public void increment(Counters counter, Processing p, String description) {
        Counter target, total;
        switch (counter) {
            case FIELD_ACCESS:
                fieldAccessGauge.process(p);
                break;
            case FOREIGN_CALL:
                foreignCallGauge.process(p);
                break;
            case SYNCHRONIZED_METHOD_ENTER:
                syncMethodEnterGauge.process(p);
                break;
            case SYNCHRONIZED_METHOD_EXIT:
                syncMethodExitGauge.process(p);
                break;
            case WAIT:
                waitGauge.process(p);
                break;
            case JOIN:
                joinGauge.process(p);
                break;
            case VOLATILE_ACCESS:
                volatileGauge.process(p);
                break;
            case CONTRACT_SYNC:
                contractSyncGauge.process(p);
                break;
            case MONITOR_ENTER:
                monEnterEnterGauge.process(p);
                break;
            case MONITOR_EXIT:
                monExitGauge.process(p);
                break;
            case RACE:
                raceGauge.process(p);
                break;
            default:
                throw new IllegalArgumentException("Unknown counter type : " + counter);
        }
    }

    public void countFieldAccess(int ordinal) {
        switch (ordinal) {
            case 0:
                detectCounter.increment();
                break;
            case 1:
                ignoreScopeCounter.increment();
                break;
            case 2:
                ignoreRuleCounter.increment();
                break;
            case 3:
                ignoreConstructorCounter.increment();
                break;
            case 4:
                ignoreFinalCounter.increment();
                break;
            case 5:
                ignoreErrorCounter.increment();
                break;
            default:
                throw new IllegalArgumentException("Unexpected ordinal : " + ordinal);
        }
    }

    public String dumpAndClear() {
        StringBuilder sb = new StringBuilder("\n================= Internal counters (processed/ignored) ===============\n");
        append("sync_method_enter", syncMethodEnterGauge, sb);
        append("sync_method_exit", syncMethodExitGauge, sb);
        append("monitor_enter", monEnterEnterGauge, sb);
        append("monitor_exit", monExitGauge, sb);
        append("wait", waitGauge, sb);
        append("join", joinGauge, sb);
        append("volatile", volatileGauge, sb);
        append("contract_sync", contractSyncGauge, sb);
        append("field_ops", fieldAccessGauge, sb);
        append("foreign_ops", foreignCallGauge, sb);
        append("races", raceGauge, sb);
        sb.append("\nField access counters\n");
        sb.append(detectCounter.getAndReset()).append(" DETECT\n");
        sb.append(ignoreScopeCounter.getAndReset()).append(" IGNORE_SCOPE\n");
        sb.append(ignoreRuleCounter.getAndReset()).append(" IGNORE_RULE\n");
        sb.append(ignoreConstructorCounter.getAndReset()).append(" IGNORE_CONSTRUCTOR\n");
        sb.append(ignoreFinalCounter.getAndReset()).append(" IGNORE_FINAL\n");
        sb.append(ignoreErrorCounter.getAndReset()).append(" IGNORE_ERROR\n");
        sb.append("Locked all time soft/hard (if available) ").append(lockedSoft.size()).append("/").append(lockedHard.size())
                .append(" of total ").append(totalMethodsTransformed.get()).append(" methods.\n");
        return sb.append("\n==============================================================================").toString();
    }

    private static void append(String entity, Gauge gauge, StringBuilder sb) {
        sb.append(entity).append(": ");
        gauge.dumpAndReset(sb);
    }

    private static class AccessCounter implements Comparable<AccessCounter> {
        private final String description;
        private final long read;
        private final long write;
        private final long total;

        private AccessCounter(String description, Gauge g) {
            this.description = description;
            read = g.getIgnored();
            write = g.getProcessed();
            total = g.getTotal();
        }

        public int compareTo(AccessCounter o) {
            return (total < o.total) ? -1 : (total > o.total ? 1 : 0);
        }
    }
}
