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

package com.devexperts.drd.transformer.transform;

import com.devexperts.drd.agent.core.InternalStatistics;
import com.devexperts.drd.bootstrap.stats.Counters;
import com.devexperts.drd.bootstrap.stats.Processing;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

import static com.devexperts.drd.transformer.instrument.InstrumentationUtils.track;

/**
 * Base transformer that adds tracking of each operation via {@link InternalStatistics} counters
 */
public class TrackingMethodTransformer implements MethodTransformer {
    protected final GeneratorAdapter mv;
    private final String description;

    public TrackingMethodTransformer(GeneratorAdapter mv, String owner, String name) {
        this.mv = mv;
        this.description = " in " + owner + "." + name;
    }

    public void processEnterSynchronizedMethod() {
        track(Counters.SYNCHRONIZED_METHOD_ENTER, Processing.PROCESSED, description, mv);
    }

    public void skipEnterSynchronizedMethod() {
        track(Counters.SYNCHRONIZED_METHOD_ENTER, Processing.IGNORED, description, mv);
    }

    public void processExitSynchronizedMethod() {
        track(Counters.SYNCHRONIZED_METHOD_EXIT, Processing.PROCESSED, description, mv);
    }

    public void skipExitSynchronizedMethod() {
        track(Counters.SYNCHRONIZED_METHOD_ENTER, Processing.IGNORED, description, mv);
    }

    public void processWait(String desc, Runnable call) {
        track(Counters.WAIT, Processing.PROCESSED, description, mv);
    }

    public void skipWait() {
        track(Counters.WAIT, Processing.IGNORED, description, mv);
    }

    public void processJoin(Runnable call) {
        track(Counters.JOIN, Processing.PROCESSED, description, mv);
    }

    public void skipJoin() {
        track(Counters.JOIN, Processing.IGNORED, description, mv);
    }

    public void processPossibleVertex(List<Integer> potentialVertices, int opcode, String owner, String name, String desc, Runnable callIfHB, Runnable callIfNotHB) {
        track(Counters.CONTRACT_SYNC, Processing.PROCESSED, description, mv);
    }

    public void skipPossibleVertex() {
        track(Counters.CONTRACT_SYNC, Processing.IGNORED, description, mv);
    }

    public void processForeignCall(int opcode, String owner, String name, String desc, int line, Runnable call) {
        track(Counters.FOREIGN_CALL, Processing.PROCESSED, description, mv);
    }

    public void skipForeignCall() {
        track(Counters.FOREIGN_CALL, Processing.IGNORED, description, mv);
    }

    public void processVolatile(int opcode, String owner, String name, String desc) {
        track(Counters.VOLATILE_ACCESS, Processing.PROCESSED, description, mv);
    }

    public void skipVolatile() {
        track(Counters.VOLATILE_ACCESS, Processing.IGNORED, description, mv);
    }

    public void processMonitorEnter() {
        track(Counters.MONITOR_ENTER, Processing.PROCESSED, description, mv);
    }

    public void skipMonitorEnter() {
        track(Counters.MONITOR_ENTER, Processing.IGNORED, description, mv);
    }

    public void processMonitorExit() {
        track(Counters.MONITOR_EXIT, Processing.PROCESSED, description, mv);
    }

    public void skipMonitorExit() {
        track(Counters.MONITOR_EXIT, Processing.IGNORED, description, mv);
    }

    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        track(Counters.FIELD_ACCESS, Processing.PROCESSED, description, mv);
    }

    public void skipFieldAccess() {
        track(Counters.FIELD_ACCESS, Processing.IGNORED, description, mv);
    }
}
