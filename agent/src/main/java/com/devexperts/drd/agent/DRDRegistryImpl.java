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

import com.devexperts.drd.agent.high_scale_lib.NonBlockingHashSet;
import com.devexperts.drd.agent.high_scale_lib.NonBlockingSetInt;
import com.devexperts.drd.bootstrap.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class DRDRegistryImpl implements DRDRegistry {
    private final Indexer<String> classes = new Indexer<String>();
    private final Indexer<String> names = new Indexer<String>();
    private final Indexer<Location> locations = new Indexer<Location>();
    private final FastIntObjMap<AtomicInteger> dataClocksRegistry = new FastIntObjMap<AtomicInteger>();
    private final Set<Integer> instrumentedClasses = new NonBlockingSetInt();
    private final Set<Integer> enums = new NonBlockingSetInt();
    private final Set<Long> finalFields = new NonBlockingHashSet<Long>();
    private final Set<Long> volatileFields = new NonBlockingHashSet<Long>();
    private final Set<Long> fields = new NonBlockingHashSet<Long>();
    private final FastIntObjMap<String> threads = new FastIntObjMap<String>();
    private final RaceHistory raceHistory;

    public DRDRegistryImpl(AccessHistory accessHistory) {
        //TODO awful cyclic dependency
        raceHistory = new RaceHistoryImpl(accessHistory, this);
        //init internals
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 1000; i++) {
            int e = random.nextInt(10, 50);
            finalFields.add((long) e);
            enums.add(e);
        }
        finalFields.clear();
        enums.clear();
        BlackHole.BLACK_HOLE.print(getClass().getSimpleName() + " initialized.");
    }

    public void registerThread(Thread thread) {
        threads.put((int)thread.getId(), thread.getName());
        DRDLogger.debug("registered thread " + thread.getName() + " (tid = " + thread.getId() + ")");
    }

    public String getThreadName(long tid) {
        return threads.get((int)tid);
    }

    public int registerClassName(String className) {
        return classes.register(className);
    }

    public String getClassName(int id) {
        return classes.get(id);
    }

    public int registerFieldOrMethodName(String name) {
        return names.register(name);
    }

    public String getFieldOrMethodName(int id) {
        return names.get(id);
    }

    public int registerLocation(int targetOwnerId, int targetNameId, int callerOwnerId, int callerNameId, int line) {
        return locations.register(new Location(targetOwnerId, targetNameId, callerOwnerId, callerNameId, line));
    }

    public Location getLocation(int id) {
        return locations.get(id);
    }

    public void registerDataClock(int ownerId) {
        AtomicInteger counter = dataClocksRegistry.get(ownerId);
        if (counter == null) {
            synchronized (dataClocksRegistry) {
                counter = dataClocksRegistry.get(ownerId);
                if (counter == null) {
                    counter = new AtomicInteger();
                    dataClocksRegistry.put(ownerId, counter);
                }
            }
        }
        counter.incrementAndGet();
    }

    public void unregisterDataClock(int ownerId) {
        dataClocksRegistry.get(ownerId).decrementAndGet();
    }

    public void registerInstrumentedClass(String className) {
        instrumentedClasses.add(registerClassName(className));
    }

    public boolean isInstrumented(int ownerId) {
        return instrumentedClasses.contains(ownerId);
    }

    public void registerFinalField(int ownerId, int fieldNameId) {
        finalFields.add(IntUtils.concat(ownerId, fieldNameId));
    }

    public boolean isFinal(int ownerId, int fieldNameId) {
        return finalFields.contains(IntUtils.concat(ownerId, fieldNameId));
    }

    public void registerVolatileField(int ownerId, int fieldNameId) {
        volatileFields.add(IntUtils.concat(ownerId, fieldNameId));
    }

    public void registerUsualField(int ownerId, int fieldNameId) {
        fields.add(IntUtils.concat(ownerId, fieldNameId));
    }

    public boolean hasField(int ownerId, int fieldNameId) {
        return fields.contains(IntUtils.concat(ownerId, fieldNameId));
    }

    public boolean isVolatile(int ownerId, int fieldNameId) {
        return volatileFields.contains(IntUtils.concat(ownerId, fieldNameId));
    }

    public void registerEnum(int ownerId) {
        enums.add(ownerId);
    }

    public boolean isEnum(int ownerId) {
        return enums.contains(ownerId);
    }

    public RaceHistory getRaceHistory() {
        return raceHistory;
    }

    public List<DataClockStats> getDataClockHistogram() {
        List<DataClockStats> set = new ArrayList<DataClockStats>();
        for (Iterator<Integer> it = dataClocksRegistry.iterator(); it.hasNext(); ) {
            final Integer next = it.next();
            set.add(new DataClockStats(getClassName(next), dataClocksRegistry.get(next).get()));
        }
        Collections.sort(set);
        return set;
    }
}