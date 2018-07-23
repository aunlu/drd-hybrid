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

import java.util.ArrayList;
import java.util.List;

public class ScopeConfig {
    public enum RuleType {INCLUDE, EXCLUDE}

    private final List<Rule> rules;
    private final boolean includeByDefault;

    ScopeConfig(List<XDRDConfig.Rule> rules, String defaultPolicy) {
        this.includeByDefault = parseRuleType(defaultPolicy).equals(RuleType.INCLUDE);
        this.rules = new ArrayList<Rule>();
        if (rules != null) {
            for (XDRDConfig.Rule r : rules) {
                this.rules.add(new Rule(ConfigUtils.toInternalName(r.path), parseRuleType(r.type)));
            }
        }
    }

    boolean shouldInclude(String s) {
        for (Rule rule : rules) {
            if (rule.matches(s)) return rule.type.equals(RuleType.INCLUDE);
        }
        return includeByDefault;
    }

    private RuleType parseRuleType(String type) {
        if ("exclude".equalsIgnoreCase(type)) return RuleType.EXCLUDE;
        if ("include".equalsIgnoreCase(type)) return RuleType.INCLUDE;
        throw new IllegalArgumentException("Unknown rule type : " + type);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ScopeConfig with default policy '").append(includeByDefault ? "include all'" : "exclude all'");
        if (rules.size() == 0) {
            sb.append(" and no special rules.\n");
        } else {
            sb.append(". Following special rules would be applied one by one until some of them would match.\n");
            for (Rule rule : rules) sb.append(rule.toString()).append("\n");
        }
        return sb.toString();
    }

    public static class Rule {
        public static final String ALL = "*";

        private final String path;
        private final RuleType type;

        public Rule(String path, RuleType type) {
            this.path = path;
            this.type = type;
        }

        private boolean matches(String s) {
            return ALL.equals(path) || s.startsWith(path);
        }

        @Override
        public String toString() {
            return type.toString() + " path '" + path + "'";
        }
    }
}
