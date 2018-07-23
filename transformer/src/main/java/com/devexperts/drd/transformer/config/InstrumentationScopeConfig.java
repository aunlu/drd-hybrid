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

import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.transformer.config.contract.AnyNameMatcher;
import com.devexperts.drd.transformer.config.contract.AnyOwnerMatcher;
import com.devexperts.drd.transformer.config.contract.OwnerNameMatcher;
import com.devexperts.drd.transformer.config.contract.TargetMatcher;

import java.util.*;

public class InstrumentationScopeConfig {
    private static InstrumentationScopeConfig INSTANCE;

    static void init(XDRDConfig.InstrumentationScope instrumentationScope) {
        INSTANCE = new InstrumentationScopeConfig(instrumentationScope);
    }

    public static InstrumentationScopeConfig getInstance() {
        return INSTANCE;
    }

    private final TargetMatcher skipFields;
    private final TargetMatcher skipForeignCalls;
    private final ScopeConfig instrScopeConfig;
    private final ScopeConfig raceDetectionScopeConfig;
    private final Set<String> alwaysExclude = new HashSet<String>();
    private final Set<String> excludeLoaded = new HashSet<String>();

    private InstrumentationScopeConfig(XDRDConfig.InstrumentationScope instrumentationScope) {
        instrScopeConfig = new ScopeConfig(instrumentationScope.syncInterception.rules, instrumentationScope.syncInterception.defaultPolicy);
        raceDetectionScopeConfig = new ScopeConfig(instrumentationScope.raceDetection.rules, instrumentationScope.raceDetection.defaultPolicy);
        skipFields = new GroupTargetMatcher(instrumentationScope.skipOurFields);
        skipForeignCalls = new GroupTargetMatcher(instrumentationScope.skipForeignCalls);
        StringBuilder sb = new StringBuilder("\nSync instrumentation scope config : ");
        sb.append(instrScopeConfig.toString());
        sb.append("\nRace detection instrumentation scope config : ");
        sb.append(raceDetectionScopeConfig.toString());
        sb.append("\nAccesses config:");
        sb.append("\n\nAccesses of following fields wouldn't be instrumented : ").append(skipFields.toString());
        sb.append("\n\nForeign calls of following methods wouldn't be instrumented : ").append(skipForeignCalls.toString());
        DRDLogger.log(sb.append("\n").toString());
    }

    public void addToExcludeList(Class[] classes) {
        for (Class c : classes) {
            excludeLoaded.add(ConfigUtils.toInternalName(c.getName()));
        }
    }

    public boolean shouldInterceptSyncOperations(String className) {
        return !ConfigUtils.containsByPrefix(alwaysExclude, className) &&
                instrScopeConfig.shouldInclude(className);
    }

    public boolean shouldInterceptDataOperations(String className) {
        return shouldInterceptSyncOperations(className) &&
                !ConfigUtils.containsByPrefix(excludeLoaded, className) &&
                raceDetectionScopeConfig.shouldInclude(className);
    }

    public RaceDetectionType shouldDetectRacesOnField(String caller, String owner, String field) {
        if (!shouldInterceptDataOperations(caller) || !shouldInterceptDataOperations(owner)) {
            return RaceDetectionType.IGNORE_SCOPE;
        }
        if (skipFields.matches(owner, field)) {
            return RaceDetectionType.IGNORE_RULE;
        }
        return RaceDetectionType.DETECT;
    }

    public boolean shouldDetectRacesOnMethodCall(String owner, String name) {
        return !ConfigUtils.containsByPrefix(alwaysExclude, owner) && !shouldInterceptDataOperations(owner) && !skipForeignCalls.matches(owner, name);
    }

    private static class GroupTargetMatcher implements TargetMatcher {
        private final List<TargetMatcher> matchers;

        public GroupTargetMatcher(List<XDRDConfig.Target> targets) {
            List<TargetMatcher> strictMatchers = new ArrayList<TargetMatcher>(targets.size());
            final Set<String> anyOwner = new HashSet<String>();
            final Set<String> anyName = new HashSet<String>();
            for (XDRDConfig.Target target : targets) {
                final String owner = ConfigUtils.toInternalName(target.owner);
                final String name = target.name;
                final boolean ownerUndefined = owner == null || owner.length() == 0 || owner.equals("*");
                final boolean nameUndefined = name == null || name.length() == 0 || name.equals("*");
                if (ownerUndefined) {
                    if (nameUndefined) {
                        throw new IllegalArgumentException("Either method owner or method name should be specified");
                    } else anyOwner.add(name);
                } else {
                    if (nameUndefined) {
                        anyName.add(owner);
                    } else {
                        strictMatchers.add(new OwnerNameMatcher(owner, name));
                    }
                }
            }
            this.matchers = new ArrayList<TargetMatcher>(strictMatchers.size() + 2);
            this.matchers.add(new AnyNameMatcher(anyName));
            this.matchers.add(new AnyOwnerMatcher(anyOwner));
            this.matchers.addAll(strictMatchers);
        }

        public boolean matches(String owner, String name) {
            for (TargetMatcher tm : matchers) {
                if (tm.matches(owner, name)) return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (TargetMatcher matcher : matchers) {
                final String str = matcher.toString();
                if (str.length() > 0) {
                    sb.append("\n").append(str).append(" \nOR");
                }
            }
            if (sb.length() >= 4) {
                sb.delete(sb.length() - 4, sb.length());
                return sb.toString();
            } else return "none";
        }
    }
}
