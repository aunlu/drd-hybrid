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

package com.devexperts.drd.transformer;

import com.devexperts.drd.bootstrap.*;
import com.devexperts.drd.transformer.config.hb.HBManager;
import com.devexperts.drd.transformer.instrument.ClassInfoCache;
import com.devexperts.drd.transformer.instrument.CompositeKeysManager;
import com.devexperts.drd.transformer.instrument.app.ApplicationTransformer;
import com.devexperts.drd.transformer.instrument.system.SystemTransformer;

import java.lang.instrument.ClassFileTransformer;

/**
 * DRDAgent loads this class by inner class loader and obtains transformer and actual implementation of {@link ICompositeKeysManager}
 */
@SuppressWarnings("UnusedDeclaration")
public class Transformation implements ITransformation {
    private final ApplicationTransformer transformer;
    private final SystemTransformer systemTransformer;

    public Transformation(ClassNameFilter classNameFilter, ClassLoader agentCL, ClassLoader transformerCL) {
        ClassInfoCache ciCache = new ClassInfoCache();
        transformer = new ApplicationTransformer(transformerCL, ciCache);
        systemTransformer = new SystemTransformer(classNameFilter, agentCL, ciCache);
        BlackHole.BLACK_HOLE.print(HBManager.getInstance());
    }

    /**
     * @return drd transformer that should be installed and used to transform classes of target application
     */
    public ClassFileTransformer getApplicationTransformer() {
        return transformer;
    }

    public DRDTransformer getSystemTransformer() {
        return systemTransformer;
    }

    /**
     * @return manager that is able to provide bytecode of dynamically generated internal DRD classes
     */
    public ICompositeKeysManager getCompositeKeysManager() {
        return CompositeKeysManager.getInstance();
    }

    /**
     * @return happens-before contracts manager
     */
    public IHBManager getHBManager() {
        return HBManager.getInstance();
    }
}
