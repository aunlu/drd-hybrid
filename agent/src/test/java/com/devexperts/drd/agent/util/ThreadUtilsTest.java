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

package com.devexperts.drd.agent.util;

import com.devexperts.drd.agent.ThreadUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThreadUtilsTest {
    @Test
    public void testGetRootThreadGroup() {
        final ThreadGroup root = ThreadUtils.getRootThreadGroup();
        assertNotNull(root);
        assertNull(root.getParent());
        assertTrue(root.parentOf(Thread.currentThread().getThreadGroup()));
    }

    @Test
    public void testGetThread() {
        checkThread(Thread.currentThread());
        for (int i = 0; i < 10; i++) {
            final Object o = new Object();
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    checkThread(Thread.currentThread());
                    synchronized (o) {
                        o.notify();
                    }
                }
            });
            assertNull(ThreadUtils.getThread(t.getId()));
            t.start();
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (o) {
                while (t.getState() == Thread.State.NEW) {
                    try {
                        o.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                checkThread(t);
            }
        }
    }

    private void checkThread(Thread thread) {
        assertEquals(ThreadUtils.getThread(thread.getId()), thread);
    }
}
