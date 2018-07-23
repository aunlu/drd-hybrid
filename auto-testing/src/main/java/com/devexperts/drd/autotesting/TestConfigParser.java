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

package com.devexperts.drd.autotesting;


import com.devexperts.drd.transformer.config.XDRDConfig;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

import java.io.*;
import java.util.*;

class TestConfigParser {
    private StringBuilder errorMessage = new StringBuilder();
    private StringBuilder warningMessage = new StringBuilder();
    private TestConfig testConfig;


    TestConfig parse(String configPath) {
        try {
            testConfig = parseTestConfig(configPath);
        } catch (Exception e) {
            errorMessage.append("ERROR: can't parse config file: ").append(e.getMessage()).append("\n");
            testConfig = null;
        }

        if (testConfig != null) {
            if (testConfig.configs.cList.size() == 0) {
                errorMessage.append("ERROR: test configs list is empty\n");
                testConfig = null;
                return testConfig;
            }

            testConfig.configNamesMap = new HashMap<>();
            testConfig.configRacesMap = new HashMap<>();
            transformConfigsPart();

            testConfig.raceConfigsMap = new HashMap<>();
            transformRacesPart();

            if (errorMessage.length() > 0) {
                testConfig = null;
            }
        }

        return testConfig;
     //   printTestConfig(testConfig);
//        logPrinter.print(AutoTesting.LONG_LINE);
    //    return testConfig;
    }


    private void transformConfigsPart() {
        if (testConfig.configs.cList == null) {
            errorMessage.append("ERROR: list of test configurations is empty\n");
            testConfig = null;
            return;
        }

        for (TestConfig.DrdConfig drdConfig: testConfig.configs.cList) {
            if (testConfig.configNamesMap.containsKey(drdConfig.cName)) {
                if (!drdConfig.cPath.equals(testConfig.configNamesMap.get(drdConfig.cName))) {
                    String oldPath = testConfig.configNamesMap.get(drdConfig.cName);
                    errorMessage.append("ERROR: test configuration contains different paths with the same name=");
                    errorMessage.append(drdConfig.cName).append(": ");
                    errorMessage.append(oldPath).append(" and ").append(drdConfig.cPath);
                    errorMessage.append("\n");
                } else {
                    //multiple similar records in config, doesn't affect test runs
                    warningMessage.append("WARNING: test configuration contains duplicate description of config with ");
                    warningMessage.append("name=").append(drdConfig.cName).append(" ");
                    warningMessage.append("path=").append(drdConfig.cPath).append("\n");
                }
            } else {
                testConfig.configNamesMap.put(drdConfig.cName, drdConfig.cPath);
                testConfig.configRacesMap.put(drdConfig.cName, new HashSet<String>());
            }
        }

        if (errorMessage.length() > 0) {
            return;
        }

        //Checking for multiple names of path
        Map<String, Set<String>> usedPaths = new HashMap<>();
        for (Map.Entry<String, String> e : testConfig.configNamesMap.entrySet()) {
            if (!usedPaths.containsKey(e.getValue())) {
                usedPaths.put(e.getValue(), new HashSet<String>());
            }
            usedPaths.get(e.getValue()).add(e.getKey());
        }
        for (String path : usedPaths.keySet()) {
            Set<String> hs = usedPaths.get(path);
            if (hs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (String name : hs) {
                    sb.append(name).append(", ");
                }
                warningMessage.append("WARNING: test configuration contains path=").append(path);
                warningMessage.append(" with multiple names: ").append(sb.substring(0, sb.length() - 2)).append("\n");
            }
        }

        checkAndTransformPaths();
    }


    private void checkAndTransformPaths() {
        for (String configName : testConfig.configNamesMap.keySet()) {
            String configPath = testConfig.configNamesMap.get(configName);
            try {
                configPath = new File(configPath).getCanonicalPath();
            } catch (IOException e) {
                errorMessage.append("ERROR: can't get canonical path for ");
                errorMessage.append(testConfig.configNamesMap.get(configName));
                errorMessage.append(": ").append(e.getMessage()).append("\n");
                continue;
            }
            if (!new File(configPath + File.separator + "config.xml").exists()) {
                errorMessage.append("ERROR: config directory (");
                errorMessage.append(testConfig.configNamesMap.get(configName));
                errorMessage.append(") doesn't contain config.xml file").append("\n");
            }
            if (!new File(configPath + File.separator + "hb-config.xml").exists()) {
                errorMessage.append("ERROR: config directory (");
                errorMessage.append(testConfig.configNamesMap.get(configName));
                errorMessage.append(") doesn't contain hb-config.xml file").append("\n");
            }
            if (!new File(configPath + File.separator + "drd.properties").exists()) {
                errorMessage.append("ERROR: config directory (");
                errorMessage.append(testConfig.configNamesMap.get(configName));
                errorMessage.append(") doesn't contain drd.properties file").append("\n");
            }
            if (errorMessage.length() == 0) {
                testConfig.configNamesMap.put(configName, configPath);
            }
        }

        if (errorMessage.length() == 0) {
            for (String configName : testConfig.configNamesMap.keySet()) {
                String configPath = testConfig.configNamesMap.get(configName) + File.separator + "config.xml";
                try {
                    parseDRDConfig(configPath);
                } catch (Exception e) {
                    errorMessage.append("ERROR: Can't parse DRDConfig file with name=").append(configName);
                    errorMessage.append(" path=").append(configPath).append("\n");
                    errorMessage.append(e.getMessage()).append("\n");
                 }
            }
        }
    }


    private void transformRacesPart() {
        if (errorMessage.length() > 0 || testConfig.races.rList == null) {
            return;
        }

        for (TestConfig.Race race : testConfig.races.rList) {
            String raceName = race.rClass + "|" + race.rField;

            if (race.rConfig != null && !testConfig.configNamesMap.containsKey(race.rConfig)) {
                errorMessage.append("ERROR: Wrong config name=").append(race.rConfig);
                errorMessage.append(" for race=").append(raceName).append("\n");
                return;
            }
            if (race.rConfig == null) {
                if (!testConfig.raceConfigsMap.containsKey(raceName)) {
                    testConfig.raceConfigsMap.put(raceName, new HashSet<String>());
                }
                if (testConfig.raceConfigsMap.get(raceName).size() == testConfig.configNamesMap.size()) {
                    warningMessage.append("WARNING: multiply description of race=").append(raceName);
                    warningMessage.append(" with all configs").append("\n");
                    continue;
                }
                Set<String> hs = testConfig.raceConfigsMap.get(raceName);
                for (String configName : testConfig.configNamesMap.keySet()) {
                    hs.add(configName);
                    testConfig.configRacesMap.get(configName).add(raceName);
                }
                testConfig.raceConfigsMap.put(raceName, hs);
                continue;
            }

            if (testConfig.raceConfigsMap.containsKey(raceName)) {
                if (testConfig.raceConfigsMap.get(raceName).contains(race.rConfig)) {
                    warningMessage.append("WARNING: test configuration contains multiply description of race with ");
                    warningMessage.append("class=").append(race.rClass).append(" ");
                    warningMessage.append("field=").append(race.rField).append(" ");
                    warningMessage.append("configurations=").append(race.rConfig).append("\n");
                } else {
                    testConfig.raceConfigsMap.get(raceName).add(race.rConfig);
                    testConfig.configRacesMap.get(race.rConfig).add(raceName);
                }
            } else {
                testConfig.raceConfigsMap.put(raceName, new HashSet<String>());
                testConfig.raceConfigsMap.get(raceName).add(race.rConfig);
                testConfig.configRacesMap.get(race.rConfig).add(raceName);
            }
        }
    }


    String getErrorMessage() {return errorMessage.toString();}


    private TestConfig parseTestConfig(String configPath) {
        XStream xStream = new XStream(new Xpp3Driver());
        Reader reader;
        try {
            reader = new FileReader(configPath);
        } catch (FileNotFoundException e) {
            errorMessage.append("ERROR: can't access config file: ").append(configPath);
            errorMessage.append(": ").append(e.getMessage()).append("\n");
            return null;
        }
        xStream.processAnnotations(TestConfig.class);
        return (TestConfig) xStream.fromXML(new BufferedReader(reader));
    }


    private XDRDConfig parseDRDConfig(String configPath) {
        XStream xStream = new XStream(new Xpp3Driver());
        Reader reader;
        try {
            reader = new FileReader(configPath);
        } catch (FileNotFoundException e) {
            errorMessage.append("ERROR: can't access DRDConfig file: ").append(configPath);
            errorMessage.append(e.getMessage());
            return null;
        }
        xStream.processAnnotations(new Class[]{XDRDConfig.class, XDRDConfig.Contracts.class,
                XDRDConfig.Target.class, XDRDConfig.SyncInterception.class, XDRDConfig.Rule.class,
                XDRDConfig.InstrumentationScope.class, XDRDConfig.Contract.class});
        return (XDRDConfig) xStream.fromXML(new BufferedReader(reader));
    }


    public void printTestConfig (SafeLogPrinter logPrinter) {
        if (warningMessage.length() > 0) {
            logPrinter.print(warningMessage.substring(0, warningMessage.length() - 1));
            logPrinter.print(AutoTesting.LONG_LINE);;
        }

        if (testConfig == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("drdConfigs:\n");
        for (Map.Entry e : testConfig.configNamesMap.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append(AutoTesting.LONG_LINE).append("\n");

        sb.append("drdRaces:").append("\n");
        if (testConfig.raceConfigsMap.size() == 0) {
            sb.append("    -").append("no races").append("\n");
        }
        for (Map.Entry<String, Set<String>> e : testConfig.raceConfigsMap.entrySet()) {
            sb.append("> ").append(e.getKey()).append("\n");
            if (e.getValue().size() == testConfig.configNamesMap.size()) {
                sb.append("    -").append("with all configs\n");
                continue;
            }
            sb.append("    -").append("with configs: ");
            for (String config : e.getValue()) {
                sb.append(config).append(", ");
            }
            sb.replace(sb.length() - 2, sb.length(), "\n");
        }
        sb.append(AutoTesting.LONG_LINE);

        logPrinter.print(sb.toString());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void buildExample() {
        XStream xStream = new XStream();
        TestConfig testTest = new TestConfig();

        TestConfig.DrdConfig c = new TestConfig.DrdConfig();
        c.cName = "simple_cfg";
        c.cPath = "../tests/config";
        testTest.configs.cList.add(c);
        c = new TestConfig.DrdConfig();
        c.cName = "contracts_cfg";
        c.cPath = "../tests/config_contracts";
        testTest.configs.cList.add(c);

        TestConfig.Race r = new TestConfig.Race();
        r.rClass  = "com.devexperts.drd_test.test1.Manager";
        r.rField  = "entities";
        testTest.races.rList.add(r);
        r = new TestConfig.Race();
        r.rClass  = "com.devexperts.drd_test.test1.User";
        r.rField  = "name";
        r.rConfig = "simple_cfg";
        testTest.races.rList.add(r);

        xStream.processAnnotations(TestConfig.class);

        System.out.println(xStream.toXML(testTest));
    }
}
