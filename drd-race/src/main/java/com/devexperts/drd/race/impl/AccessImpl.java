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

import com.devexperts.drd.race.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@XmlType(propOrder = {"accessType", "targetInfo", "tid", "threadName", "codeLine", "stackTrace", "threadClock", "targetClock"})
@XmlAccessorType(XmlAccessType.FIELD)
public class AccessImpl implements Access {
    private CodeLine codeLine;
    private AccessType accessType;
    private String threadName;
    private StackTrace stackTrace;
    @XmlElement(type=ThreadClockImpl.class)
    private ThreadClock threadClock;
    @XmlElement(type=DataClockImpl.class)
    private DataClock targetClock;
    private long tid;
    private Map<String, String> targetInfo = new HashMap<String, String>();

    public CodeLine getCodeLine() {
        return codeLine;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public Map<String, String> getTargetInfo() {
        return targetInfo;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getTid() {
        return tid;
    }

    public StackTrace getStackTrace() {
        return stackTrace;
    }

    public ThreadClock getThreadClock() {
        return threadClock;
    }

    public DataClock getTargetClock() {
        return targetClock;
    }

    public void setCodeLine(CodeLine codeLine) {
        this.codeLine = codeLine;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void setStackTrace(StackTrace stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setThreadClock(ThreadClock threadClock) {
        this.threadClock = threadClock;
    }

    public void setTargetClock(DataClock targetClock) {
        this.targetClock = targetClock;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public void setTargetInfo(Map<String, String> targetInfo) {
        this.targetInfo = targetInfo;
    }

    public void addTargetInfo(String key, String value) {
        targetInfo.put(key, value);
    }

    @Override
    public String toString() {
        return "AccessImpl{" +
                "codeLine=" + codeLine +
                ", accessType=" + accessType +
                ", threadName='" + threadName + '\'' +
                ", threadClock=" + threadClock +
                ", targetClock=" + targetClock +
                ", tid=" + tid +
                ", targetInfo=" + targetInfo +
                ", stackTrace='" + stackTrace + '\'' +
                '}';
    }
}
