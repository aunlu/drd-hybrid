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

package com.devexperts.drd.transformer.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ConfigUtilsTest {
    @Test
    public void testContainsByPrefix() {
        Assert.assertFalse(ConfigUtils.containsByPrefix(Arrays.<String>asList(), "abc"));
        Assert.assertFalse(ConfigUtils.containsByPrefix(Arrays.asList("def"), "abc"));
        Assert.assertTrue(ConfigUtils.containsByPrefix(Arrays.asList("def", "abc"), "abc"));
    }

    @Test
    public void testCompileSimpleCfgString() {
        Pattern p = ConfigUtils.compileFromCfgString("abc", true);
        assertMatch(p, "abcdef", "abc123def");
        assertNotMatch(p, "", "fgh", "ab");
    }

    @Test
    public void testCompileFromSingleCfgString() {
        Pattern p = ConfigUtils.compileFromCfgString("abc*def", true);
        Assert.assertEquals(p.pattern(), ConfigUtils.compileFromCfgString("abc*def*", false).pattern());
        Assert.assertEquals(p.pattern(), ConfigUtils.compileFromCfgString("abc*def*", true).pattern());
        assertMatch(p, "abcdef", "abc123def", "abcdeflalala", "abc123deflalala");
        assertNotMatch(p, "", "fgh", "abcef");
        p = ConfigUtils.compileFromCfgString("abc*def", false);
        assertMatch(p, "abcdef", "abc123def");
        assertNotMatch(p, "", "fgh", "abcef", "abcdeflalala", "abc123deflalala");
    }

    @Test
    public void testCompileFromMultiCfgString() {
        Pattern p = ConfigUtils.compileFromCfgString("abc*def,123*456", true);
        assertMatch(p, "abcdef", "abc123def", "abcdeflalala", "abc123deflalala", "123456", "123789456", "12384569999");
        assertNotMatch(p, "", "fgh", "abcef", "abc456", "123fghdef");
        p = ConfigUtils.compileFromCfgString("abc*def|123*456", false);
        assertMatch(p, "abcdef", "abc123def", "123456", "123789456");
        assertNotMatch(p, "", "fgh", "abcef", "abcdeflalala", "abc123deflalala","12384569999");
    }

    private void assertMatch(Pattern p, String... strings) {
        for (String s : strings) Assert.assertTrue(p.matcher(s).matches());
    }

    private void assertNotMatch(Pattern p, String... strings) {
        for (String s : strings) Assert.assertFalse(p.matcher(s).matches());
    }
}
