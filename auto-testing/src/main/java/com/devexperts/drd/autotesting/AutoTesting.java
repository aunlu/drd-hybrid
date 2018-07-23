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

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.devexperts.drd.autotesting.TestResult.*;
import static java.lang.Math.min;

public class AutoTesting {
    static final String LONG_LINE = "--------------------------------------------------------------";

    //test format: TEST_PATH/TEST_PACKAGE.testName
    //test directory should contain TestRunner.java and config.xml
    static final String TESTS_PACKAGE = "com.devexperts.drd.tests.examples";
    static final String TESTS_PATH = "./tests/src/main/java".replace("/", File.separator);

    //test format: COMPILED_TEST_PATH/TEST_PACKAGE.testName
    //tests are already compiled to this folder thanks to maven
    //this directory contains only TestRunner.class file
    static final String COMPILED_TESTS_PATH = "./tests/target/classes".replace("/", File.separator);
    static final String LOG_DIRECTORY = "./tests/target/auto-logs".replace("/", File.separator);

    private static List<TestResult> testResults = new ArrayList<>();

    private static boolean allIncluded = true;
    private static boolean allExcluded = true;
    private static Set<String> includedTestsSet = new HashSet<>();
    private static Set<String> excludedTestsSet = new HashSet<>();



    private static void processTestIncluding() {
        String includedTests = System.getProperty("drd.auto.testing.include", "*");
        String excludedTests = System.getProperty("drd.auto.testing.exclude", "");
        if (!includedTests.equals("*")) {
            allIncluded = false;
            for (String testName : includedTests.split(",")) {
                includedTestsSet.add(testName);
            }
        }
        if (!excludedTests.equals("*")) {
            allExcluded = false;
            for (String testName : excludedTests.split(",")) {
                excludedTestsSet.add(testName);
            }
        }
        if (allIncluded && allExcluded) {
            allIncluded = false;
        }
    }


    private static TestResult runTest(File f, String testName, SafeLogPrinter logPrinter) {
        if (new File(f.toString() + File.separator + testName).isFile()) {
            return new TestResult(ResultType.ERROR, "ERROR: should be directory, not file");
        }

        String abs;
        try {
            abs = f.getCanonicalPath();
        } catch (IOException e) {
            return new TestResult(ResultType.ERROR, "ERROR: cannot get canonical path: " + e.getMessage());
        }

        String confPath = abs + File.separator + testName + File.separator + "config.xml";
        if (!new File(confPath).exists()) {
            return new TestResult(ResultType.ERROR, "ERROR: test config not found");
        }

        TestConfigParser testConfigParser = new TestConfigParser();
        TestConfig conf = testConfigParser.parse(confPath);
        testConfigParser.printTestConfig(logPrinter);
        if (conf == null) {
            return new TestResult(ResultType.ERROR, testConfigParser.getErrorMessage());
        }

        TestRunner testRunner = new TestRunner(testName, conf);
        return testRunner.runTest();
    }



    private static TestResult createWriterAndRunTest(File f, String testName, BufferedWriter writer) {
        BufferedWriter bTestWriter = null;

        String testLogPath = LOG_DIRECTORY + File.separator + testName;
        String testLogFile = testLogPath + File.separator + "log.txt";
        try {
            if (!new File(testLogPath).mkdirs() || !new File(testLogFile).createNewFile()) {
                SafeLogPrinter.print(writer, "ERROR: can't create path to " + testName + " test log file");
                return new TestResult(ResultType.ERROR, "ERROR: can't create path to " + testName + " test log file");
            }
            bTestWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testLogFile), "UTF-8"));
            SafeLogPrinter logPrinter = new SafeLogPrinter(writer, bTestWriter);
            TestResult result = runTest(f, testName, logPrinter);
            logPrinter.print(result);
            return result;
        } catch (IOException e) {
            SafeLogPrinter.print(writer, "ERROR: can't open " + testName + " test log file " + e.getMessage());
            return new TestResult(ResultType.ERROR, "ERROR: can't open " + testName + " test log file " + e.getMessage());
        } finally {
            closeQuietly(bTestWriter);
        }
    }


    private static void doTestsFrom(String s, BufferedWriter writer) {
        File f = new File(s);
        if (f.isFile()) {
            System.err.println("Wrong tests path. Should be directory: " + f.toString());
            return;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(f.toPath())) {
            File[] files = f.listFiles();
            if (files == null) {
                System.err.println("Wrong tests path (not directory) " + f.toString() + " or I/O error occurred");
                return;
            }

            processTestIncluding();

            int i = 1;
            for (Path p : ds) {
                String testName = p.toString().replace(s + File.separator, "");

                SafeLogPrinter.print(writer, i + " of " + files.length + ": [" + testName + "]");
                SafeLogPrinter.print(writer, AutoTesting.LONG_LINE);
                if (!allExcluded && !excludedTestsSet.contains(testName) &&
                                    (allIncluded || includedTestsSet.contains(testName))) {
                    testResults.add(createWriterAndRunTest(f, testName, writer));
                } else {
                    testResults.add(new TestResult(ResultType.SKIPPED, ""));
                    SafeLogPrinter.print(writer, "TEST SKIPPED\n");
                }
                testResults.get(testResults.size() - 1).testName = testName;

                SafeLogPrinter.print(writer, "\n");
                i++;
            }
        } catch (IOException e) {
            System.err.println("Can't access files in " + s + " :" + e.getMessage());
        }
    }

    private static void cleanLogFiles(File f) {
        if (!f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                cleanLogFiles(file);
            }
            if (!f.delete()) {
                System.out.println("Can't delete old log file: " + f.toString());
            }
        } else {
            if (!f.delete()) {
                System.out.println("Can't delete old logs directory: " + f.toString());
            }
        }
    }


    private static void printResults(BufferedWriter writer) {
        if (testResults.size() == 0) {
            return;
        }

        int minLen = testResults.get(0).testName.length();
        for (TestResult res : testResults) {
            minLen = min(res.testName.length(), minLen);
        }

        StringBuilder sb = new StringBuilder();
        String points = ".............................";

        for (TestResult res : testResults) {
            sb.append(res.testName);
            sb.append(points.substring(res.testName.length() - minLen));
            sb.append(res.type.toString());
            sb.append("\n");
        }

        SafeLogPrinter.print(writer, sb.toString());
    }

    private static void createJUnitReport(BufferedWriter writer) {
        BufferedWriter xmlWriter = null;
        String xmlFile = LOG_DIRECTORY + File.separator + "report.xml";
        String TAB = "    ";

        try {
            if (!new File(xmlFile).createNewFile()) {
                SafeLogPrinter.print(writer, "ERROR: can't create path to JUnit report file");
                return;
            }
            xmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8"));
            SafeLogPrinter.print(writer, "JUnit report file path: " + xmlFile + "\n\n");

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");

            int skips = 0, errors = 0, failures = 0;
            for (TestResult testResult : testResults) {
                switch (testResult.type) {
                    case SKIPPED:   skips++;
                                    break;
                    case ERROR:     errors++;
                                    break;
                    case FAILED:    failures++;
                                    break;
                }
            }

            sb.append("<testsuite ").append("name=\"drd-tests\" ");
            sb.append("tests=\"").append(testResults.size()).append("\" ");
            sb.append("skips=\"").append(skips).append("\" ");
            sb.append("errors=\"").append(errors).append("\" ");
            sb.append("failures=\"").append(failures).append("\" ");
            sb.append(">\n");

            StringBuilder tab = new StringBuilder(TAB);
            for (TestResult testResult : testResults) {
                sb.append(tab).append("<testcase ").append("name=\"").append(testResult.testName).append("\" ");
                sb.append(">\n");
                tab.append(TAB);
                switch (testResult.type) {
                    case OK:        sb.append(tab).append("<system-out>\n");
                                    sb.append(testResult.message);
                                    sb.append(tab).append("</system-out>\n");
                                    break;
                    case SKIPPED:   sb.append(tab).append("<skipped/>\n");
                                    break;
                    case ERROR:     sb.append(tab).append("<error message=\"");
                                    sb.append(testResult.message);
                                    sb.append(tab).append("\" />\n");
                                    break;
                    case FAILED:    sb.append(tab).append("<failure message=\"test failed\">\n");
                                    sb.append(testResult.message);
                                    sb.append(tab).append("</failure>\n");
                                    break;
                }
                tab.replace(tab.length() - TAB.length(), tab.length(), "");
                sb.append(tab).append("</testcase>\n\n");
            }


            sb.append("</testsuite>");

            xmlWriter.write(sb.toString());
            xmlWriter.flush();
        } catch (IOException e) {
            System.out.println("ERROR: can't open log file " + e.getMessage());
        } finally {
            closeQuietly(xmlWriter);
        }
    }


    public static void main(String[] args) {
 //     TestConfigParser.buildExample();


        System.out.println("\n\n" + LONG_LINE);
        System.out.println(":::::::::::::::::::::::: AUTO-TESTING ::::::::::::::::::::::::");
        System.out.println(LONG_LINE + "\n");


        //preparing log output
        String logFile = LOG_DIRECTORY + File.separator + "log.txt";
        BufferedWriter bWriter = null;
        try {
            cleanLogFiles(new File(LOG_DIRECTORY));
            if (!new File(LOG_DIRECTORY).mkdirs() || !new File(logFile).createNewFile()) {
                System.out.println("ERROR: can't create path to log file");
                return;
            }
            bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"));

            System.out.println("Log path: " + LOG_DIRECTORY );
            //tests_walker
            String testsPath = TESTS_PATH + File.separator + TESTS_PACKAGE.replace(".", File.separator);
            SafeLogPrinter.print(bWriter, "Tests directory path: " + new File(testsPath).getAbsoluteFile());
            SafeLogPrinter.print(bWriter, "\n");
            doTestsFrom(testsPath, bWriter);

            printResults(bWriter);
            createJUnitReport(bWriter);

        } catch (IOException e) {
            System.out.println("ERROR: can't open log file " + e.getMessage());
        } finally {
            closeQuietly(bWriter);
        }
    }

    private static void closeQuietly(Closeable writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            //skip
        }
    }
}