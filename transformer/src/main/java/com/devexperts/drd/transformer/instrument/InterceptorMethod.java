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

package com.devexperts.drd.transformer.instrument;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import static com.devexperts.drd.transformer.instrument.Constants.*;
import static org.objectweb.asm.Type.*;

public enum InterceptorMethod {
    FIELD_WRITE(new Method("afterWrite", VOID_TYPE, new Type[]{IDATACLOCK_TYPE, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE})),
    FIELD_READ(new Method("afterRead", VOID_TYPE, new Type[]{IDATACLOCK_TYPE, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE})),
    FOREIGN_WRITE(new Method("beforeForeignWrite", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE})),
    FOREIGN_READ(new Method("afterForeignRead", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE})),
    BEFORE_WAIT(new Method("beforeWait", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    AFTER_WAIT(new Method("afterWait", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    MONITOR_EXIT(new Method("beforeMonitorExit", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    MONITOR_ENTER(new Method("afterMonitorEnter", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    VOLATILE_READ(new Method("afterVolatileRead", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    VOLATILE_WRITE(new Method("beforeVolatileWrite", VOID_TYPE, new Type[]{OBJECT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    MANUAL_SYNC_SEND(new Method("beforeManualSyncSend", VOID_TYPE, new Type[]{ABSTRACT_WEAK_DISPOSABLE_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    MANUAL_SYNC_RECEIVE(new Method("afterManualSyncReceive", VOID_TYPE, new Type[]{ABSTRACT_WEAK_DISPOSABLE_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    MANUAL_SYNC_FULL(new Method("afterManualSyncFullHB", VOID_TYPE, new Type[]{ABSTRACT_WEAK_DISPOSABLE_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE})),
    STATUS(new Method("status", INT_TYPE, EMPTY_TYPE_ARRAY)),
    LOCK_SOFT(new Method("lockSoft", INT_TYPE, EMPTY_TYPE_ARRAY)),
    UNLOCK_SOFT(new Method("unlockSoft", INT_TYPE, EMPTY_TYPE_ARRAY)),
    LOCK_HARD(new Method("lockHard", INT_TYPE, EMPTY_TYPE_ARRAY)),
    UNLOCK_HARD(new Method("unlockHard", INT_TYPE, EMPTY_TYPE_ARRAY)),
    THREAD_JOIN(new Method("afterJoin", VOID_TYPE, new Type[]{THREAD_TYPE})),
    THREAD_START(new Method("beforeStart", VOID_TYPE, new Type[]{THREAD_TYPE})),
    THREAD_DEATH(new Method("beforeDying", VOID_TYPE, EMPTY_TYPE_ARRAY));

    final Method method;

    InterceptorMethod(Method method) {
        this.method = method;
    }
}
