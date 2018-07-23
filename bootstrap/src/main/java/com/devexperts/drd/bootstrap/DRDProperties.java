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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class DRDProperties {
    public enum LoggingLevel {
        INFO(20),DEBUG(30);

        private final int level;

        private LoggingLevel(int level) {
            this.level = level;
        }
    }
    public enum RacesGrouping {CALL_LOCATION, CALL_CLASS, CALL_CLASS_AND_METHOD}
    public enum DebugTransformMode {SYSTEM, APPLICATION}
    public enum Metrics {FLAG_ONLY, FLAG_IGNORE, TRACK_NO_PROCESS, PROCESS_NO_TRACK, FAKE_GUARDED_INTERCEPTOR, FULL}

    private static final String SETTINGS_FILE = "drd.properties";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String DEFAULT_CONFIG_DIR = "config";

    private static final Properties properties;
    public static final boolean soutEnabled = false;
    public static final boolean profilingEnabled;
    public static final LoggingLevel threshold;
    public static final RacesGrouping racesGrouping;
    public static final DebugTransformMode debugTransformMode;
    public static final Metrics metrics;
    public static final int dataClockHistogramLimit;
    public static final boolean reportForeignRaces;

    static {
        properties = new Properties(System.getProperties());
        try {
            String cfgFilePath = properties.getProperty("drd.settings.file");
            InputStream cfgFile = null;
            if (cfgFilePath != null) {
                try {
                    cfgFile = new FileInputStream(cfgFilePath);
                } catch (FileNotFoundException e) {
                    System.out.println("file '" + cfgFilePath + "' not found!");
                }
            }
            if (cfgFile != null) {
                loadProperties(cfgFilePath, cfgFile);
            } else {
                System.out.println("Searching " + SETTINGS_FILE + " in default directory ...");
                cfgFile = ClassLoader.getSystemClassLoader().getResourceAsStream(SETTINGS_FILE);
                if (cfgFile != null) {
                    loadProperties(SETTINGS_FILE, cfgFile);
                } else throw new IOException("Settings file '" + SETTINGS_FILE + " not found. Using defaults.");
            }
        } catch (IOException e) {
            System.out.println("WARN: Failed to load '" + SETTINGS_FILE + "'.");
        }
        threshold = LoggingLevel.valueOf(getStringProperty("drd.log.level", "INFO"));
        profilingEnabled = getBooleanProperty("drd.profiling.enabled", true);
        reportForeignRaces = getBooleanProperty("drd.report.foreign.races", true);
        dataClockHistogramLimit = Math.max(20, getIntProperty("drd.data.clock.histogram.limit", 20));
        racesGrouping = RacesGrouping.valueOf(getStringProperty("drd.races.grouping", "CALL_CLASS_AND_METHOD"));
        debugTransformMode = DebugTransformMode.valueOf(getStringProperty("drd.debug.transform.mode", "APPLICATION"));
        metrics = Metrics.valueOf(getStringProperty("drd.internal.metrics", "FULL"));
    }

    static String dumpSettings() {
        StringBuilder sb = new StringBuilder("--------------------DRD settings:----------------------\n");
        sb.append("logging_level = ").append(threshold).append("\n");
        sb.append("races_grouping = ").append(racesGrouping).append("\n");
        sb.append("report_races_on_foreign_calls = ").append(reportForeignRaces).append("\n");
        sb.append("debugTransformMode = ").append(debugTransformMode).append("\n");
        sb.append("data_clock_histogram_size = ").append(dataClockHistogramLimit).append("\n");
        sb.append("internal_profiling_enabled = ").append(profilingEnabled).append("\n");
        sb.append("internal_metrics = ").append(metrics).append("\n");
        return sb.append("-------------------------------------------------------").toString();
    }

    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getMostRelevantProperty(key);
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = getMostRelevantProperty(key);
        return value == null ? defaultValue : Integer.valueOf(value);
    }

    private static String getStringProperty(String key, String defaultValue) {
        String value = getMostRelevantProperty(key);
        return value == null ? defaultValue : value;
    }

    private static String getMostRelevantProperty(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            System.out.println("Property overridden from command line: " + key + "=" + value);
            return value;
        }
        return properties.getProperty(key);
    }

    private static void loadProperties(String description, InputStream is) throws IOException {
        properties.load(is);
        System.out.println("-- DRD properties loaded from file " + description + ": --");
        for (Enumeration e = properties.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (key.startsWith("drd.")) {
                System.out.println(key + "=" + properties.get(key));
            }
        }
        System.out.println("----------------------------------------------");
    }

    public static boolean enabled(LoggingLevel level) {
        return threshold.level >= level.level;
    }

    public static String getLogDir() {
        return properties.getProperty("drd.log.dir", DEFAULT_LOG_DIR);
    }

    public static String getTransformedFilesDir() {
        return properties.getProperty("drd.transformed.files.dir", null);
    }

    public static String getConfigDir() {
        return properties.getProperty("drd.config.dir", DEFAULT_CONFIG_DIR);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key, "");
    }
}
