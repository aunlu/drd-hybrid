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

package com.devexperts.drd.transformer.instrument.app;

import com.devexperts.drd.bootstrap.DRDProperties;
import com.devexperts.drd.transformer.transform.*;
import org.objectweb.asm.commons.GeneratorAdapter;

public class TransformerFactory {
    public static MethodTransformer createTransformer(GeneratorAdapter mv, int access, String owner, String name) {
        DRDProperties.Metrics metrics = DRDProperties.metrics;
        switch (metrics) {
            case FULL:
            case FAKE_GUARDED_INTERCEPTOR:
            //case FAKE_INTERCEPTOR:
                //both transformation levels work
                return new ProcessingMethodTransformer(mv, access, owner, name, new TrackingMethodTransformer(mv, owner, name));
            case PROCESS_NO_TRACK:
                //only processing on 2nd level, no tracking
                return new ProcessingMethodTransformer(mv, access, owner, name, new EmptyTransformer());
            case TRACK_NO_PROCESS:
                //only tracking on 2nd level, no processing
                return new IdentityMethodTransformer(mv, new TrackingMethodTransformer(mv, owner, name));
            case FLAG_IGNORE:
            case FLAG_ONLY:
                //only 1nd level works
                return new IdentityMethodTransformer(mv, new EmptyTransformer());
            default:
                throw new IllegalArgumentException("Unknown internal metrics: " + metrics);
        }
    }
}