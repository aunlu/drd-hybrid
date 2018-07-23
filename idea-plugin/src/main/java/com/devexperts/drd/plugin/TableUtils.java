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

package com.devexperts.drd.plugin;

import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.CodeLine;
import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.RaceTargetType;
import com.devexperts.drd.race.impl.StackTrace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class TableUtils {
    public static String convertDateToReadable(Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        return sdf.format(time);
    }

    public static String convertRaceTargetTypeToReadable(Race race) {
        RaceTargetType type = race.getRaceTargetType();
        Map<String, String> targetInfo = race.getCurrentAccess().getTargetInfo();
        StringBuilder result = new StringBuilder();

        switch (type) {
            case FIELD:
                result.append(type.name()).append(' ');
                result.append(targetInfo.get(Access.FIELD_OWNER));
                result.append('.').append(targetInfo.get(Access.FIELD_NAME));
                break;
            case OBJECT:
                result.append(type.name()).append(' ');
                result.append(targetInfo.get(Access.OBJECT_TYPE));
                break;
            default:
                result.append("Unknown race target type");
        }

        return result.toString();
    }

    public static String convertAccessToReadable(Race race, Access access) {
        StringBuilder result = new StringBuilder(access.getAccessType().name());

        if (race.getRaceTargetType() == RaceTargetType.OBJECT) {
            Map<String, String> targetInfo = access.getTargetInfo();
            result.append(' ').append(targetInfo.get(Access.OBJECT_TYPE))
                    .append('.').append(targetInfo.get(Access.OBJECT_METHOD));
        }

        result.append(getStackTrace(access.getStackTrace(), "\n", false));
        return result.toString();
    }

    public static String getTooltipStackTrace(Access access) {
        String result = getStackTrace(access.getStackTrace(), "<br/>", false);

        if ("".equals(result)) {
            CodeLine line = access.getCodeLine();
            return " at " + line.getClassName() + "." + line.getMethodName() + " (line: " + line.getLine() + ")";
        } else {
            result = "<html>" + result + "</html>";
        }

        return result;
    }

    public static String getFullStackTrace(Access access) {
        String result = getStackTrace(access.getStackTrace(), "\n", true);

        if ("".equals(result)) {
            CodeLine line = access.getCodeLine();
            return " at " + line.getClassName() + "." + line.getMethodName() + " (line: " + line.getLine() + ")";
        }

        return result;
    }

    private static String getStackTrace(StackTrace stackTrace, String delimiter, boolean full) {
        StringBuilder result = new StringBuilder();

        if (stackTrace != null && stackTrace.getElements() != null) {
            for (StackTraceElement stackTraceElement : stackTrace.getElements()) {
                result.append(" at ")
                        .append(full ? stackTraceElement : shortenPackageName(stackTraceElement.toString()))
                        .append(delimiter);
            }
        }

        return result.toString();
    }

    private static String shortenPackageName(String fullName) {
        String[] s = fullName.split("/|\\.");

        if (s.length <= 4) {
            return fullName;
        }

        StringBuilder result = new StringBuilder(s[0] + ".");
        boolean shorten = true;

        for (int i = 1; i < s.length; i++) {
            String p = s[i];
            boolean isLast = i + 1 < s.length;

            if (shorten && isLast && s[i + 1].contains("(")) {
                shorten = false;
            }

            if (shorten) {
                result.append(p.substring(0, 1)).append('.');
            } else {
                result.append(p);

                if (isLast) {
                    result.append('.');
                }
            }
        }

        return result.toString();
    }
}
