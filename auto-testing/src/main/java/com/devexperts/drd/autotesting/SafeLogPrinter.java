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

import java.io.BufferedWriter;
import java.io.IOException;

class SafeLogPrinter {
    static void print(BufferedWriter writer, String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
            System.out.print(message + "\n");
        } catch (IOException e) {
            System.err.print("IOException happened: " + e.getMessage() + "\n");
            System.err.print("Cannot write result to the log file: " + message + "\n");
            e.printStackTrace();
        }
    }

    private BufferedWriter writer, testWriter;

    SafeLogPrinter(BufferedWriter writer, BufferedWriter testWriter) {
        this.writer = writer;
        this.testWriter = testWriter;
    }

    void print(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
            testWriter.write(message + "\n");
            testWriter.flush();
            System.out.print(message + "\n");
        } catch (IOException e) {
            System.err.print("IOException happened: " + e.getMessage() + "\n");
            System.err.print("Cannot write result to the log file: " + message + "\n");
            e.printStackTrace();
        }
    }

    void print(TestResult res) {
        print(res.message + "\n");
    }
}