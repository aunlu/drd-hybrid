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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtilsTest {
    @Test
    public void testPutToMapOfMapsOfLists() {
        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
        CollectionUtils.putToMapOfMapsOfLists("outer1", "inner1", "value111", map);
        CollectionUtils.putToMapOfMapsOfLists("outer1", "inner1", "value112", map);
        CollectionUtils.putToMapOfMapsOfLists("outer1", "inner2", "value121", map);
        CollectionUtils.putToMapOfMapsOfLists("outer2", "inner1", "value211", map);
        CollectionUtils.putToMapOfMapsOfLists("outer2", "inner2", "value221", map);
        Assert.assertEquals(map.size(), 2);
        Assert.assertEquals(map.get("outer1").size(), 2);
        Assert.assertEquals(map.get("outer1").get("inner1").size(), 2);
        Assert.assertEquals(map.get("outer1").get("inner1").get(0), "value111");
        Assert.assertEquals(map.get("outer1").get("inner1").get(1), "value112");
        Assert.assertEquals(map.get("outer1").get("inner2").size(), 1);
        Assert.assertEquals(map.get("outer1").get("inner2").get(0), "value121");
        Assert.assertEquals(map.get("outer2").size(), 2);
        Assert.assertEquals(map.get("outer2").get("inner1").size(), 1);
        Assert.assertEquals(map.get("outer2").get("inner1").get(0), "value211");
        Assert.assertEquals(map.get("outer2").get("inner2").size(), 1);
        Assert.assertEquals(map.get("outer2").get("inner2").get(0), "value221");
    }
}
