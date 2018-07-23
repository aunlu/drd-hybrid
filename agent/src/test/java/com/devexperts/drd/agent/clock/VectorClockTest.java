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

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class VectorClockTest {
    private final Random random = new Random();

    @Test
    public void testCreateRandomVC() {
        for (int i = 1; i < 1000; i++) {
            int maxTid = random.nextInt(i * 2) + 1;
            int maxFrame = i * i;
            final VectorClock vc = createRandomVC(i, maxTid, maxFrame);
            if (vc == null) {
                Assert.assertTrue(maxTid < i);
            } else {
                Assert.assertTrue(vc.size == i * 2);
                for (int j = 0; j < vc.size; j += 2) {
                    Assert.assertTrue(vc.clock[j] <= maxTid);
                    Assert.assertTrue(vc.clock[j + 1] <= maxFrame);
                }
            }
        }
    }

    @Test
    public void testCopy() {
        for (int i = 1; i < 1000; i++) {
            int maxTid = random.nextInt(i * 2) + 1;
            int maxFrame = i * i;
            final VectorClock vc = createRandomVC(i, maxTid, maxFrame);
            if (vc != null) {
                Assert.assertTrue(VectorClockUtils.same(vc, new VectorClock(vc)));
            }
        }
    }

    @Test
    public void testAddTid() {
        final VectorClock vc = new VectorClock();
        Assert.assertEquals(vc.size, 0);
        for (int i = 1; i < 10000; i++) {
            vc.setFrame(i, i * 2);
            Assert.assertEquals(vc.size, i * 2);
            Assert.assertEquals(vc.getFrame(i), i * 2);
        }
    }

    @Test
    public void testTick() {
        for (int i = 2; i < 200; i++) {
            int maxTid = i + 1;
            int maxFrame = i * i;
            final long tid = Thread.currentThread().getId();
            final VectorClock vc = createRandomVC(i, maxTid, maxFrame);
            if (vc != null && vc.getFrame(tid) > 0) {
                final ThreadVectorClock tvc = new ThreadVectorClock();
                for (int j = 0; j < vc.size / 2; j++) {
                    tvc.setFrame(vc.clock[2 * j], vc.clock[2 * j + 1]);
                }
                tvc.tick();
                Assert.assertEquals(vc.size, tvc.size);
                for (int j = 0; j < vc.size; j += 2) {
                    Assert.assertEquals(vc.clock[j], tvc.clock[j]);
                    Assert.assertEquals(vc.clock[j + 1], vc.clock[j] == tid ? tvc.clock[j + 1] - 1 : tvc.clock[j + 1]);
                }
            }
        }
    }

    protected VectorClock createRandomVC(int length, int maxTid, int maxFrame) {
        final VectorClock vc = new VectorClock();
        if (maxTid < length) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            int tid;
            do {
                tid = random.nextInt(maxTid) + 1;
            } while (VectorClockUtils.findTid(vc, tid) >= 0);
            vc.setFrame(tid, random.nextInt(maxFrame + 1));
        }
        return vc;
    }
}
