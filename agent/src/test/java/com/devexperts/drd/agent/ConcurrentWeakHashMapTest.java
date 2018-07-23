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

import com.devexperts.drd.agent.util.ConcurrentWeakHashMap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ConcurrentWeakHashMapTest {
    @Test
    @Ignore("Fails sometimes - fix it.")
    public void testWeakness() {
        ConcurrentWeakHashMap<Object, String> map = new ConcurrentWeakHashMap<Object, String>(EqualsComparison.INSTANCE);
        Object[] objects = new Object[10000];
        for (int i = 0; i < 10000; i++) {
            map.put(objects[i] = new Object(), String.valueOf(i));
        }
        for (int i = 0; i < 10000; i+=2) {
            objects[i] = null;
        }
        System.gc();
        System.gc();
        for (int i = 0; i < 10000; i++) {
            map.put(new Object(), String.valueOf(i));
        }
        Assert.assertEquals(15000, map.size());
    }
}
