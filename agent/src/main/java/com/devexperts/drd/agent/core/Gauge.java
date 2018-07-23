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
import com.devexperts.drd.bootstrap.stats.Processing;

/**
 * Tracker of event processing based on three high-performance precise thread-safe counters.
 */
public class Gauge {
    private final Counter processed = new Counter();
    private final Counter ignored = new Counter();
    private final Counter total = new Counter();

    public void process(Processing p) {
        switch (p) {
            case PROCESSED:
                processed.increment();
                total.increment();
                break;
            case IGNORED:
                ignored.increment();
                total.increment();
                break;
            default:
                throw new IllegalArgumentException("Unknown processing type : " + p);
        }
    }

    /**
     * Appends counters to sb and resets them
     *
     * @param sb builder
     * @return sb
     */
    public StringBuilder dumpAndReset(StringBuilder sb) {
        return sb.append(processed.estimateGetAndReset()).append("/").append(ignored.estimateGetAndReset()).append("=").append(total.estimateGetAndReset()).append("\n");
    }


    /**
     * Appends counters to sb and resets them
     *
     * @param sb builder
     * @return sb
     */
    public StringBuilder dump(StringBuilder sb) {
        return sb.append(processed.estimate_get()).append("/").append(ignored.estimate_get()).append("=").append(total.estimate_get()).append("\n");
    }

    public long getProcessed() {
        return processed.estimate_get();
    }

    public long getIgnored() {
        return ignored.estimate_get();
    }

    public long getTotal() {
        return total.estimate_get();
    }
}
