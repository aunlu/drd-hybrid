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

import com.devexperts.drd.autotesting.TestResult.ResultType;
import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.RaceTargetType;
import com.devexperts.drd.race.io.RaceIO;
import com.devexperts.drd.race.io.RaceIOException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestRunner {
    private static final String AGENT_PATH = "./bin/target/drd_agent.jar";

    private String testName;
    private TestConfig conf;
    //private SafeLogPrinter logPrinter;
    private String testPath, logDirPath;
    private TestResult testResult;
    private StringBuilder log;


    TestRunner(String testName, TestConfig conf) {
        this.testName = testName;
        this.conf = conf;
        this.log = new StringBuilder();
    }

    TestResult runTest() {
        try {
            testPath = new File(AutoTesting.COMPILED_TESTS_PATH).getCanonicalPath();
            logDirPath = new File(AutoTesting.LOG_DIRECTORY).getCanonicalPath();
        } catch (IOException e) {
            return new TestResult(ResultType.ERROR, "ERROR: cannot get canonical path: " + e.getMessage());
        }

        String compiledTestPath = testPath + File.separator + AutoTesting.TESTS_PACKAGE.replace(".", File.separator)
                + File.separator + testName + File.separator + "TestRunner.class";
        if (!new File(compiledTestPath).exists()) {
            return new TestResult(ResultType.ERROR, "ERROR: Compiled test file doesn't exists: " + compiledTestPath);
        }

        testResult = new TestResult(ResultType.OK, "");
        testResult.setNumberOfRuns(conf.configNamesMap.size());

        int i = 0;
        for (Map.Entry<String, String> e : conf.configNamesMap.entrySet()) {
            log.append("Run ").append(testName).append(" with configuration ").append(e.getKey()).append("\n");

            String drdLogPath;
            drdLogPath = logDirPath + File.separator + testName + File.separator + e.getKey();
            try {
                drdLogPath = new File(drdLogPath).getCanonicalPath();
            } catch (IOException excep) {
                testResult.registerError();
                log.append("ERROR: Cannot get canonical path for log path: ").append(e.getValue()).append("\n");

                i++;
                if (i < conf.configNamesMap.entrySet().size()) {
                    log.append(AutoTesting.LONG_LINE).append("\n");
                }
                continue;
            }

            String errors = runConfiguration(drdLogPath, e.getValue());
            if (errors.length() > 0) {
                testResult.registerError();
                log.append(errors);
            } else {
                String res = checkResult(drdLogPath, e.getKey());
                log.append(res);
                if (res.contains("ERROR")) {
                    testResult.registerError();
                } else {
                    if (res.contains("FAILURE")) {
                        testResult.registerFailure();
                    } else {
                        log.append("OK: Test result matches to current config\n");
                    }
                }
            }

            i++;
            if (i < conf.configNamesMap.entrySet().size()) {
                log.append(AutoTesting.LONG_LINE).append("\n");
            }
        }

        testResult.setMessage(log.toString());
        return testResult;
    }


    private String runConfiguration(String logPath, String confPath) {
        StringBuilder sb = new StringBuilder();

        try {
            String r = "java " + "-javaagent:" + AGENT_PATH + " "
                               + "-Ddrd.log.dir=" + logPath + " "
                               + "-Ddrd.config.dir=" + confPath + " "
                        //     + "-Ddrd.properties.dir=" + confPath + " "
                        //     + "-Ddrd.transformed.files.dir=./auto-testing/target/files/" + testName + "/ "
                               + "-cp " + testPath + " " + AutoTesting.TESTS_PACKAGE + "." + testName + ".TestRunner";

            Process process = Runtime.getRuntime().exec(r);
            process.waitFor();
            int res = process.exitValue();
            if (res != 0) {
                sb.append("ERROR: Run failed with exit code: ").append(res).append("\n");
                sb.append("Run command was:").append("\n");
                sb.append(r).append("\n");
            }
        } catch (Exception e) {
            sb.append("ERROR: Run failed with exception: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }


    private String checkResult(String logPath, String confName) {
        logPath += File.separator + "drd_races.log";

        try {
            logPath = new File(logPath).getCanonicalPath();
        } catch (IOException excep) {
            return "ERROR: Cannot get canonical path: " + logPath;
        }

        List<Race> races;
        try {
            races = RaceIO.read(logPath);
        } catch (RaceIOException e) {
            return "ERROR: Cannot parse log file " + logPath;
        }

        StringBuilder sb = new StringBuilder();

        Set<String> expectedRaces = new HashSet<>(conf.configRacesMap.get(confName));
        Set<String> foundRaces = new HashSet<>();
        for (Race race : races) {
            foundRaces.add(transformRaceToString(race, TransformType.COMPARE));
        }

        sb.append(getRacesInformation(expectedRaces, foundRaces)).append("\n");

        StringBuilder errors = new StringBuilder();
        for (String foundRace : foundRaces) {
            if (expectedRaces.contains(foundRace)) {
                expectedRaces.remove(foundRace);
            } else {
                if (errors.length() == 0) {
                    errors.append("FAILURE: Unexpected races found:\n");
                }
                errors.append("       > ").append(foundRace).append("\n");
            }
        }
        if (expectedRaces.size() > 0) {
            errors.append("FAILURE: Expected races not found:\n");
            for (String expectedRace : expectedRaces) {
                errors.append("       > ").append(expectedRace).append("\n");
            }
        }

        sb.append(errors);
        return sb.toString();
    }


    private String getRacesInformation(Set<String> expectedRaces, Set<String> foundRaces) {
        StringBuilder sb = new StringBuilder();

        if (expectedRaces.size() > 1) {
            sb.append("Expected ").append(expectedRaces.size()).append(" races:");
        } else {
            if (expectedRaces.size() == 1) {
                sb.append("Expected ").append(expectedRaces.size()).append(" race:");
            } else {
                sb.append("Races not expected");
            }
        }
        sb.append("\n");
        for (String expectedRace : expectedRaces) {
            sb.append("       > ").append(expectedRace).append("\n");
        }

        if (foundRaces.size() > 1) {
            sb.append("Found ").append(foundRaces.size()).append(" races:");
        } else {
            if (foundRaces.size() == 1) {
                sb.append("Found ").append(foundRaces.size()).append(" race:");
            } else {
                sb.append("Races not found");
            }
        }
        sb.append("\n");
        for (String race : foundRaces) {
            sb.append("       > ").append(race).append("\n");
        }

        return sb.substring(0, sb.length() - 1);
    }


    private enum TransformType {
        OUTPUT, COMPARE
    }

    private String transformRaceToString(Race logRace, TransformType t) {
        StringBuilder sb = new StringBuilder();
        Access access = logRace.getRacingAccess();

        RaceTargetType raceType;
        String type_or_owner;
        String method_or_name;

        if (logRace.getRaceTargetType() == RaceTargetType.OBJECT) {
            //OBJECT
            raceType = RaceTargetType.OBJECT;
            type_or_owner = access.getTargetInfo().get("type");
            method_or_name = access.getTargetInfo().get("method");
        } else {
            //FIELD
            raceType = RaceTargetType.FIELD;
            type_or_owner = access.getTargetInfo().get("owner");
            method_or_name = access.getTargetInfo().get("name");
        }

        if (t == TransformType.OUTPUT) {
            if (raceType == RaceTargetType.OBJECT) {
                sb.append("object-race with ");
                if (type_or_owner != null) {
                    sb.append("type=").append(new File(type_or_owner).toString()).append(" ");
                }
                if (method_or_name != null) {
                    sb.append("method=").append(new File(method_or_name).toString()).append(" ");
                }
            } else {
                sb.append("field-race with ");
                if (type_or_owner != null) {
                    sb.append("owner=").append(new File(type_or_owner).toString()).append(" ");
                }
                if (method_or_name != null) {
                    sb.append("name=").append(new File(method_or_name).toString()).append(" ");
                }
            }
        } else {
            if (type_or_owner != null) {
                sb.append(new File(type_or_owner).toString());
            }
            if (method_or_name != null) {
                if (type_or_owner != null) {
                    sb.append("|");
                }
                sb.append(new File(method_or_name).toString());
            }
        }

        return sb.toString().replace(File.separator, ".");
    }
}