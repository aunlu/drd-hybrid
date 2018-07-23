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

package com.devexperts.drd.race.impl;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;

@XmlAccessorType(XmlAccessType.FIELD)
public class StackTrace {
    @XmlJavaTypeAdapter(StackTraceElementAdapter.class)
    @XmlElement(name="at")
    private StackTraceElement[] elements;

    public StackTrace(StackTraceElement[] elements) {
        this.elements = elements;
    }

    public StackTrace() {
    }

    public StackTraceElement[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return "StackTrace{" +
                "elements=" + Arrays.toString(elements) +
                '}';
    }
}
