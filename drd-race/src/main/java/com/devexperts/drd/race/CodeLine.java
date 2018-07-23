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

package com.devexperts.drd.race;

import javax.xml.bind.annotation.XmlType;

/**
 * Holds information about certain line of code: class.method:line.
 */
@SuppressWarnings("unused")
@XmlType(propOrder = {"className", "methodName", "line"})
public class CodeLine {
    private String className;
    private String methodName;
    private int line;

    public CodeLine(String className, String methodName, int line) {
        this.className = className;
        this.methodName = methodName;
        this.line = line;
    }

    public CodeLine() {
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setLine(int line) {
        this.line = line;
    }

    /**
     * @return line number
     */
    public int getLine() {
        return line;
    }

    /**
     * @return class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return "CodeLine{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", line=" + line +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodeLine codeLine = (CodeLine) o;

        if (line != codeLine.line) return false;
        if (!className.equals(codeLine.className)) return false;
        return methodName.equals(codeLine.methodName);

    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + line;
        return result;
    }
}
