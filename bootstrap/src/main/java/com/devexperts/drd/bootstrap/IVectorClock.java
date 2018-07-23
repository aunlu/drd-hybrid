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

package com.devexperts.drd.bootstrap;

public interface IVectorClock {
    long[] EMPTY_ARRAY = new long[0];

    /**
     * @param tid thread id
     * @return time frame of specified thread, stored in this clock, or -1
     */
    long getFrame(long tid);
    
    int getFrame2(long tid);

    long[] getLiveFrames();

    long[] getDeadFrames();
}
