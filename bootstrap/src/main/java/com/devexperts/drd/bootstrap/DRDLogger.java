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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DRDLogger {
    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";
    private static final String DEBUG_FILE = "drd_debug.log";
    private static final String LOG_FILE = "drd.log";
    private static final String ERR_FILE = "drd_error.log";
    private static final String RACES_FILE = "drd_races.log";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_FORMAT);
        }
    };

    private static BufferedWriter debugWriter;
    private static BufferedWriter logWriter;
    private static BufferedWriter errorWriter;
    public static File racesFile;

    static void setLogDir(String dirName) {
        final File logDir = new File(dirName);
        //noinspection ResultOfMethodCallIgnored
        logDir.mkdirs();
        final File debugFile = new File(logDir, DEBUG_FILE);
        final File logFile = new File(logDir, LOG_FILE);
        final File errFile = new File(logDir, ERR_FILE);
        debugWriter = createWriter(debugFile, "debug");
        logWriter = createWriter(logFile, "log");
        errorWriter = createWriter(errFile, "errors");
        racesFile = new File(logDir, RACES_FILE);
        if (!racesFile.exists()) {
            try {
                racesFile.createNewFile();
                System.out.println("DRD races file: " + racesFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    DRDLogger.error("Failed to create '" + racesFile.getCanonicalPath() + "'", e);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    DRDLogger.error("", e1);
                }
                DRDLogger.error("Failed to create races file!");
            }
        }
    }

    public static void debug(String s) {
        if (DRDProperties.enabled(DRDProperties.LoggingLevel.DEBUG)) {
            write(s, debugWriter, System.out);
        }
    }

    public static void debug(String s, Throwable e) {
        if (DRDProperties.enabled(DRDProperties.LoggingLevel.DEBUG)) {
            debug(toString(s, e));
        }
    }

    public static void log(Object o) {
        log(o.toString());
    }

    public static void log(String s) {
        write(s, logWriter, System.out);
    }

    public static void log(String message, Throwable e) {
        log(toString(message, e));
    }

    public static void error(Object o) {
        error(o.toString());
    }

    public static void error(String s) {
        System.err.println(s);
        log(s);
        write(s, errorWriter, System.err);
    }

    public static void errorWithStackTrace(String s) {
        error(s, new Throwable());
    }

    public static void error(String message, Throwable e) {
        error(toString(message, e));
    }

    private static void write(String s, BufferedWriter writer, PrintStream alt) {
        if (writer == null) {
            alt.println(getCurrentTimeStamp() + " " + s);
            return;
        }
        try {
            writer.write(getCurrentTimeStamp() + " " + s + LINE_SEPARATOR);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedWriter createWriter(File file, String type) {
        BufferedWriter writer = null;
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            writer = new BufferedWriter(new FileWriter(file, true));
            System.out.println("DRD " + type + " file: " + file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            try {
                DRDLogger.error("Failed to create '" + file.getCanonicalPath() + "'", e);
            } catch (IOException e1) {
                e1.printStackTrace();
                DRDLogger.error("", e1);
            }
        }
        return writer;
    }

    private static String toString(String s, Throwable e) {
        PrintWriter pw;
        try {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String res = s + System.getProperty("line.separator") + sw.toString();
            pw.close();
            return res;
        } catch (Throwable e1) {
            String res = "Exception of type " + e.getClass().getName() + " with message '" + e.getMessage()
                    + "' and user message '" + s + "'";
            error("Failed to log " + res);
            return res;
        }
    }

    private static String getCurrentTimeStamp() {
        return formatter.get().format(new Date());
    }
}
