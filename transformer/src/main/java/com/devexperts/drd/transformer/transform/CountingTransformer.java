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

package com.devexperts.drd.transformer.transform;

import com.devexperts.drd.transformer.instrument.InstrumentationUtils;
import org.objectweb.asm.commons.GeneratorAdapter;

public class CountingTransformer extends DelegateTransformer {
    private final GeneratorAdapter mv;

    public CountingTransformer(GeneratorAdapter mv) {
        super(new IdentityMethodTransformer(mv, new EmptyTransformer()));
        this.mv = mv;
    }

    @Override
    public void processFieldAccess(int opcode, String owner, String name, String desc, int line) {
        super.processFieldAccess(opcode, owner, name, desc, line);
        InstrumentationUtils.trackFieldAccess(owner + "." + name, InstrumentationUtils.isWrite(opcode), mv);
    }
}