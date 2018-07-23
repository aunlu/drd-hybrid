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

package com.devexperts.drd.gui;

import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.CodeLine;
import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.RaceTargetType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Util {
    private static final ThreadLocal<DateFormat> formatters = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        }
    };
    public static final int MAX_STACKTRACE_SIZE = 20;

    private Util() {
    }

    public static String getDescription(Access access, RaceTargetType type, boolean html) {
        StringBuilder sb = new StringBuilder();
        if (html) {
            sb.append("<html>");
        }
        sb.append(access.getAccessType().name()).append(" @ ");
        sb.append(access.getThreadName()).append("  ");
        if (type == RaceTargetType.OBJECT) {
            sb.append(access.getTargetInfo().get(Access.OBJECT_TYPE)).append(".")
                    .append(access.getTargetInfo().get(Access.OBJECT_METHOD)).append("()");
        }
        if (html) {
            sb.append("<br/><br/>").append(getStackTrace(access, "<br/>", false, true)).append("</html>");
        } else {
            sb.append(getStackTrace(access, "\n", true, true));
        }
        return sb.toString();
    }

    private static String getStackTrace(Access access, String separator, boolean compactPackageNames, boolean compactStackTrace) {
        if (access.getStackTrace() == null || access.getStackTrace().getElements() == null) {
            CodeLine line = access.getCodeLine();
            String cl = line.getClassName() + "." + line.getMethodName() + " (line: " + line.getLine() + ")";
            return " at " + (compactPackageNames ? shortenPackageName(cl) : cl);
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (StackTraceElement traceElement : access.getStackTrace().getElements()) {
            sb.append(" at ").append(compactPackageNames ? shortenPackageName(traceElement.toString()) : traceElement).append(separator);
            if (compactStackTrace && ++i > MAX_STACKTRACE_SIZE) {
                sb.append("... and ").append(access.getStackTrace().getElements().length - MAX_STACKTRACE_SIZE).append(" more ...");
                break;
            }
        }
        return sb.toString();
    }

    public static String getRawStackTrace(Access access) {
        return getStackTrace(access, "\n", false, false);
    }

    public static String shortenPackageName(String fullName) {
        String[] s = fullName.split("/|\\.");
        if (s.length <= 4) {
            return fullName;
        }
        StringBuilder sb = new StringBuilder(s[0] + ".");
        boolean shorten = true;
        for (int i = 1; i < s.length; i++) {
            String p = s[i];
            boolean isLast = i + 1 < s.length;
            if (shorten && isLast && s[i + 1].contains("(")) {
                shorten = false;
            }
            if (shorten) {
                sb.append(p.substring(0, 1)).append(".");
            } else {
                sb.append(p);
                if (isLast) {
                    sb.append(".");
                }
            }
        }
        return sb.toString();
    }

    public static String getTarget(Race race) {
        StringBuilder sb = new StringBuilder(race.getRaceTargetType().name()).append(" ");
        Map<String, String> currentInfo = race.getCurrentAccess().getTargetInfo();
        Map<String, String> racingInfo = race.getRacingAccess().getTargetInfo();
        switch (race.getRaceTargetType()) {
            case FIELD:
                sb.append(currentInfo.get(Access.FIELD_OWNER)).append(".").append(currentInfo.get(Access.FIELD_NAME));
                break;
            case OBJECT:
                String currentObject = currentInfo.get(Access.OBJECT_TYPE);
                String racingObject = racingInfo.get(Access.OBJECT_TYPE);
                String currentMethod = currentInfo.get(Access.OBJECT_METHOD);
                String racingMethod = racingInfo.get(Access.OBJECT_METHOD);
                if (racingObject.equals(currentObject)) {
                    sb.append(currentObject).append(" ").append(currentMethod).append("() vs ")
                            .append(racingMethod).append("()");
                } else {
                    sb.append(currentObject).append(".").append(currentMethod).append("() vs ")
                            .append(racingObject).append(".").append(racingMethod).append("()");
                }
                break;
            default:
                throw new IllegalStateException("Unknown race target type : " + race.getRaceTargetType());
        }
        return sb.toString();
    }

    public static String format(Date date) {
        return formatters.get().format(date);
    }
}
