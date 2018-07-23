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

package com.devexperts.drd.agent.clock;

import com.devexperts.drd.bootstrap.DRDLogger;

public class GenerationUtils {
    private GenerationUtils() {
    }

    public static int[] propagateGeneration(int[] from, int[] to) {
        if (from != to) {
            int oldBitCount = getBitCount(to);
            int min = from.length < to.length ? from.length : to.length;
            if (to.length <= from.length) {
                boolean strictlyIn = true;
                for (int i = 0; i < min; i++) {
                    if ((~from[i] & to[i]) != 0) {
                        strictlyIn = false;
                        break;
                    }
                }
                if (strictlyIn) {
                    return from;
                }
            }
            int[] res = new int[to.length];
            for (int i = 0; i < min; i++) {
                res[i] = from[i] | to[i];
            }
            System.arraycopy(to, min, res, min, to.length - min);
            int newBitCount = getBitCount(res);
            if (newBitCount > oldBitCount) {
                DRDLogger.log((newBitCount - oldBitCount) + " bits of dead info propagated!");
            }
            return res;
        } else return to;
    }

    public static int[] updateMask(int[] mask, int[] add) {
        if (add.length == 0) return mask;
        int max = 0;
        for (int tid : add) {
            if (tid > max) max = tid;
        }
        int[] res = new int[max > 0 && max / 32 + 1 > mask.length ? max / 32 + 1 : mask.length];
        System.arraycopy(mask, 0, res, 0, mask.length);
        for (int tid : add) {
            res[tid / 32] |= (1 << (tid % 32));
        }
        return res;
    }

    private static int getBitCount(int[] mask) {
        int res = 0;
        for (int i : mask) {
            res += Integer.bitCount(i);
        }
        return res;
    }
}
