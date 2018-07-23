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

package com.devexperts.drd.race;

import com.devexperts.drd.race.impl.*;
import com.devexperts.drd.race.io.RaceIO;
import com.devexperts.drd.race.io.RaceIOException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceIOTest {
    @Test
    public void testIOFile() throws RaceIOException {
        List<Race> races = RaceIO.read(RaceIOTest.class.getResourceAsStream("drd_races.log"));
        Assert.assertEquals(races.size(), 19);
    }

    @Test
    public void testIO() throws RaceIOException {
        Race race = createRace();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RaceIO.write(race, baos);
        byte[] buf = baos.toByteArray();
        List<Race> read = RaceIO.read(new ByteArrayInputStream(buf));
        Assert.assertEquals(read.size(), 1);
        assertRacesSame(race, read.get(0));
    }

    private void assertRacesSame(Race r1, Race r2) {
        Assert.assertEquals(r1.getRaceTargetType(), r2.getRaceTargetType());
        Assert.assertEquals(r1.getTime().getTime(), r2.getTime().getTime());
        assertAccessesSame(r1.getCurrentAccess(), r2.getCurrentAccess());
        assertAccessesSame(r1.getRacingAccess(), r2.getRacingAccess());
    }

    private void assertAccessesSame(Access a1, Access a2) {
        Assert.assertEquals(a1.getTargetClock(), a2.getTargetClock());
        Assert.assertEquals(a1.getThreadClock(), a2.getThreadClock());
        Assert.assertEquals(a1.getAccessType(), a2.getAccessType());
        Assert.assertArrayEquals(a1.getStackTrace().getElements(), a2.getStackTrace().getElements());
        Assert.assertEquals(a1.getThreadName(), a2.getThreadName());
        Assert.assertEquals(a1.getTid(), a2.getTid());
        Assert.assertEquals(a1.getTargetInfo(), a2.getTargetInfo());
    }

    private Race createRace() {
        RaceImpl race = new RaceImpl();
        AccessImpl current = new AccessImpl();
        current.setThreadName("T1");
        current.setCodeLine(new CodeLine("abc", "def", 1));
        current.setAccessType(AccessType.WRITE);
        current.setStackTrace(new StackTrace(Thread.currentThread().getStackTrace()));
        current.setTargetClock(new DataClockImpl(new long[]{1, 2}, new long[]{3, 4}));
        current.setThreadClock(new ThreadClockImpl(new long[]{5, 6}, new long[]{7, 8}));
        current.setTid(12);
        current.setTargetInfo(asMap(Access.FIELD_NAME, "f", Access.FIELD_OWNER, "cl"));

        AccessImpl racing = new AccessImpl();
        racing.setThreadName("T2");
        racing.setCodeLine(new CodeLine("ghi", "jkl", 1));
        racing.setAccessType(AccessType.READ);
        racing.setStackTrace(new StackTrace());
        racing.setTargetClock(new DataClockImpl(new long[]{10, 11}, new long[]{12, 13}));
        racing.setThreadClock(new ThreadClockImpl(new long[]{14, 15}, new long[]{16, 17}));
        racing.setTid(34);
        racing.setTargetInfo(asMap(Access.FIELD_NAME, "f2", Access.FIELD_OWNER, "cl2"));

        race.setTime(new Date());
        race.setRacingAccess(racing);
        race.setCurrentAccess(current);
        race.setRaceTargetType(RaceTargetType.FIELD);
        return race;
    }

    private Map<String, String> asMap(String ... elements) {
        Map<String, String> map = new HashMap<String, String>(elements.length / 2);
        for (int i = 0; i < elements.length; i+=2) {
            map.put(elements[i], elements[i+1]);
        }
        return map;
    }
}
