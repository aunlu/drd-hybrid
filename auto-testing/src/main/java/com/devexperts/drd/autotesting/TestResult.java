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

public class TestResult {
    enum ResultType {OK, FAILED, ERROR, SKIPPED}

    ResultType type;
    String testName, message;
    private int runs, errors, failures;

    TestResult(ResultType type, String message) {
        this.type = type;
        this.message = message;
        runs = 0; errors = 0; failures = 0;
    }

    void setNumberOfRuns(int n) {runs = n;}

    void registerError() {
        type = ResultType.ERROR;
        errors++;
    }

    void registerFailure() {
        if (type == ResultType.OK) {
            type = ResultType.FAILED;
        }
        failures++;
    }

    void setMessage(String s) {
        message = s;
    }
}