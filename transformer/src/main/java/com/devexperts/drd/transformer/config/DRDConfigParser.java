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

package com.devexperts.drd.transformer.config;

import com.devexperts.drd.bootstrap.DRDConfig;
import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.DRDProperties;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

import java.io.*;

public class DRDConfigParser {
    private static final String CONFIG_FILE = "config.xml";

    public static DRDConfig parseConfig() {
        XStream xStream = new XStream(new Xpp3Driver());
        xStream.processAnnotations(new Class[]{XDRDConfig.class, XDRDConfig.Contracts.class,
                XDRDConfig.Target.class, XDRDConfig.SyncInterception.class, XDRDConfig.Rule.class,
                XDRDConfig.InstrumentationScope.class, XDRDConfig.Contract.class});
        final String cfgFileName = DRDProperties.getConfigDir() + File.separator + CONFIG_FILE;
        DRDLogger.log("Trying to find '" + cfgFileName + "' as file ...");
        Reader reader = null;
        try {
            reader = new FileReader(cfgFileName);
        } catch (FileNotFoundException e) {
            DRDLogger.log(e.getMessage());
        }
        if (reader == null) {
            DRDLogger.log("Trying to find '" + cfgFileName + "' as resource ...");
            reader = new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream(CONFIG_FILE));
        }
        XDRDConfig cfg = (XDRDConfig) xStream.fromXML(new BufferedReader(reader));
        InstrumentationScopeConfig.init(cfg.instrumentationScope);
        return new DRDConfigImpl(new ContractsConfig(cfg.contracts), new TraceConfig(cfg.traceTracking));
    }
}
