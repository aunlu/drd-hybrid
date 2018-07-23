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

import com.devexperts.drd.agent.high_scale_lib.NonBlockingHashMapLong;
import com.devexperts.drd.bootstrap.AccessHistory;
import com.devexperts.drd.bootstrap.IntUtils;
import com.devexperts.drd.bootstrap.ThreadRaceInfo;

public class AccessHistoryImpl implements AccessHistory {
    //TODO use simplify, fix concurrency bugs
    private final NonBlockingHashMapLong<NonBlockingHashMapLong<ThreadRaceInfo>> fieldTraces;
    private final NonBlockingHashMapLong<NonBlockingHashMapLong<ThreadRaceInfo>> objectTraces;

    public AccessHistoryImpl() {
        fieldTraces = new NonBlockingHashMapLong<NonBlockingHashMapLong<ThreadRaceInfo>>();
        objectTraces = new NonBlockingHashMapLong<NonBlockingHashMapLong<ThreadRaceInfo>>();
    }

    public void saveFieldAccess(int ownerId, int nameId, long tid, ThreadRaceInfo raceInfo) {
        long key = IntUtils.concat(ownerId, nameId);
        NonBlockingHashMapLong<ThreadRaceInfo> map = fieldTraces.get(key);
        if (map == null) {
            map = new NonBlockingHashMapLong<ThreadRaceInfo>();
            NonBlockingHashMapLong<ThreadRaceInfo> old = fieldTraces.putIfAbsent(key, map);
            if (old != null) {
                map = old;
            }
        }
        map.put(tid, raceInfo);
    }

    public void saveObjectAccess(Object o, int ownerId, long tid, ThreadRaceInfo raceInfo) {
        long key = IntUtils.concat(ownerId, System.identityHashCode(o));
        NonBlockingHashMapLong<ThreadRaceInfo> map = objectTraces.get(key);
        if (map == null) {
            map = new NonBlockingHashMapLong<ThreadRaceInfo>();
            NonBlockingHashMapLong<ThreadRaceInfo> old = objectTraces.putIfAbsent(key, map);
            if (old != null) {
                map = old;
            }
        }
        map.put(tid, raceInfo);
    }

    public ThreadRaceInfo getFieldAccess(int ownerId, int nameId, long tid) {
        NonBlockingHashMapLong<ThreadRaceInfo> map = fieldTraces.get(IntUtils.concat(ownerId, nameId));
        return map == null ? null : map.get(tid);
    }

    public ThreadRaceInfo getObjectAccess(Object o, int ownerId, long tid) {
        long key = IntUtils.concat(ownerId, System.identityHashCode(o));
        NonBlockingHashMapLong<ThreadRaceInfo> map = objectTraces.get(key);
        return map == null ? null : map.get(tid);
    }
}
