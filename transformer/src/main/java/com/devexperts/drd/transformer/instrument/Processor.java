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

/**
 * Interface for handling three typical situations that can occur when there some logic that handles arbitrary events
 * <ul>
 *     <li>Event should be <b>processed</b> - i.e., handled by logic and executed</li>
 *     <li>Event should be <b>skipped</b> - i.e. fact of skipping may be optionally tracked by logic and event should be executed</li>
 *     <li>Event should be <b>executed</b> - i.e. direct execution with any acting of logic</li>
 * </ul>
 * From the external point of view (i.e., JVM stack machine, business logic, etc) the result of all these three types of handling
 * should be exactly the same as if target event was simple executed.
 */
public abstract class Processor {
    /**
     * Process event. Should have same effect on stack as if event was simply executed.
     */
    public abstract void process();

    /**
     * Skip event. Should have same effect on stack as if event was simply executed.
     */
    public final void skip() {
        trackSkip();
        execute();
    }

    /**
     * (Optionally) track the fact that event was skipped. Should have no effect on call stack from external point of view.
     */
    protected abstract void trackSkip();

    /**
     * Execute event. Has same effect on stack as if event was simply executed.
     */
    public abstract void execute();
}
