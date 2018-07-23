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

package com.devexperts.drd.agent.race;

import com.devexperts.drd.agent.ThreadUtils;
import com.devexperts.drd.bootstrap.*;
import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.AccessType;
import com.devexperts.drd.race.CodeLine;
import com.devexperts.drd.race.RaceTargetType;
import com.devexperts.drd.race.impl.*;
import com.devexperts.drd.race.io.RaceIO;
import com.devexperts.drd.race.io.RaceIOException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class RaceReporter {
    private static final StackTraceElement[] NO_STACKTRACE = new StackTraceElement[0];
    private DRDRegistry registry;

    public RaceReporter(DRDRegistry registry) {
        this.registry = registry;
    }

    public synchronized void reportRace(DataRaceDescription.RaceTarget target, DataRaceDescription race, ThreadRaceInfo racingThreadInfo) {
        RaceImpl r = new RaceImpl();
        r.setTime(new Date());
        RaceTargetType raceTargetType = convertRaceTarget(target);
        r.setRaceTargetType(raceTargetType);

        AccessImpl currentAccess = createAccess(race.currentLocationId, raceTargetType);
        currentAccess.setAccessType(convertAccessType(race.currentAccess));
        currentAccess.setTid(Thread.currentThread().getId());
        currentAccess.setThreadName(Thread.currentThread().getName());
        currentAccess.setTargetClock(new DataClockImpl(race.targetVC.getReadFrames(), race.targetVC.getWriteFrames()));
        currentAccess.setThreadClock(new ThreadClockImpl(race.currentThreadVC.getLiveFrames(), race.currentThreadVC.getDeadFrames()));
        currentAccess.setStackTrace(new StackTrace(ThreadUtils.removeTopDRDCalls(Thread.currentThread().getStackTrace())));
        r.setCurrentAccess(currentAccess);

        AccessImpl racingAccess = createAccess(race.racingLocationId, raceTargetType);
        racingAccess.setAccessType(convertAccessType(race.racingAccess));
        racingAccess.setTid(race.racingTid);
        racingAccess.setThreadName(registry.getThreadName(race.racingTid));
        racingAccess.setTargetClock(null); //not available
        racingAccess.setThreadClock(null); //not available
        if (racingThreadInfo != null) {
            racingAccess.setStackTrace(new StackTrace(ThreadUtils.removeTopDRDCalls(racingThreadInfo.exception.getStackTrace())));
        } else {
            racingAccess.setStackTrace(new StackTrace(NO_STACKTRACE)); //not available
        }
        r.setRacingAccess(racingAccess);
        try {
            FileOutputStream out = new FileOutputStream(DRDLogger.racesFile, true);
            RaceIO.write(r, out);
            DRDLogger.log("Race detected and logged to " + DRDLogger.racesFile.getName());
            out.close();
        } catch (RaceIOException e) {
            DRDLogger.error("Failed to write race\n" + r, e);
        } catch (FileNotFoundException e) {
            DRDLogger.error("Failed to write race: file " + DRDLogger.racesFile.getName() + " not found.\n" + r, e);
        } catch (IOException e) {
            DRDLogger.error("Failed to write race\n" + r, e);
        }
    }

    private AccessImpl createAccess(int location, RaceTargetType raceTargetType) {
        AccessImpl access = new AccessImpl();
        Location l = registry.getLocation(location);
        access.setCodeLine(new CodeLine(registry.getClassName(l.callerOwnerId), registry.getFieldOrMethodName(l.callerNameId), l.line));
        switch (raceTargetType) {
            case FIELD:
                access.addTargetInfo(Access.FIELD_OWNER, registry.getClassName(l.targetOwnerId));
                access.addTargetInfo(Access.FIELD_NAME, registry.getFieldOrMethodName(l.targetNameId));
                break;
            case OBJECT:
                access.addTargetInfo(Access.OBJECT_TYPE, registry.getClassName(l.targetOwnerId));
                access.addTargetInfo(Access.OBJECT_METHOD, registry.getFieldOrMethodName(l.targetNameId));
                break;
        }
        return access;
    }

    private AccessType convertAccessType(DataRaceDescription.AccessType type) {
        switch (type) {
            case READ:
                return AccessType.READ;
            case WRITE:
                return AccessType.WRITE;
            default:
                throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }

    private RaceTargetType convertRaceTarget(DataRaceDescription.RaceTarget target) {
        switch (target) {
            case FIELD:
                return RaceTargetType.FIELD;
            case OBJECT:
                return RaceTargetType.OBJECT;
            default:
                throw new IllegalArgumentException("Unexpected target: " + target);
        }
    }
}
