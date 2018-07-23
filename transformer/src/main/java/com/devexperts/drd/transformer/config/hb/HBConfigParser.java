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

package com.devexperts.drd.transformer.config.hb;

import com.devexperts.drd.bootstrap.DRDLogger;
import com.devexperts.drd.bootstrap.DRDProperties;
import com.devexperts.drd.transformer.config.ConfigUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HBConfigParser {
    private static final String CONFIG_FILE = "hb-config.xml";

    static List<HBContract> parseConfig() {
        XStream xstream = new XStream(new Xpp3Driver());
        xstream.processAnnotations(new Class[]{HappensBeforeConfig.class, Sync.class, Send.class, Receive.class, MethodCall.class,
                Link.class, MultiLink.class, MultiSync.class, Call.class});
        Reader reader = null;
        final String cfgFileName = DRDProperties.getConfigDir() + File.separator + CONFIG_FILE;
        DRDLogger.log("Trying to find '" + cfgFileName + "' as file ...");
        try {
            reader = new FileReader(cfgFileName);
        } catch (FileNotFoundException e) {
            DRDLogger.log(e.getMessage());
        }
        if (reader == null) {
            DRDLogger.log("Trying to find '" + cfgFileName + "' as resource ...");
            reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream(CONFIG_FILE)));
        }
        try {
            final HappensBeforeConfig config = (HappensBeforeConfig) xstream.fromXML(reader);
            if (config.syncs == null) config.syncs = new ArrayList<Sync>();
            if (config.multiSyncs == null) config.multiSyncs = new ArrayList<MultiSync>();
            final List<HBContract> hbContracts = new ArrayList<HBContract>(config.syncs.size());
            for (Sync sync : config.syncs) {
                final HBContract hbContract = new HBContract();
                final List<Integer> sendIndices = new ArrayList<Integer>(sync.links.size());
                final List<Integer> receiveIndices = new ArrayList<Integer>(sync.links.size());
                for (Link link : sync.links) {
                    switch (link.send) {
                        case param:
                            sendIndices.add(link.sendNumber);
                            break;
                        case owner:
                            sendIndices.add(-1);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown link type : " + link.send.toString());
                    }
                    switch (link.receive) {
                        case param:
                            receiveIndices.add(link.receiveNumber);
                            break;
                        case owner:
                            receiveIndices.add(-1);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown link type : " + link.receive.toString());
                    }
                }
                if (sync.Send.MethodCall.equals(sync.Receive.MethodCall) && sendIndices.equals(receiveIndices)) {
                    hbContract.addVertex(SynchronizationPointType.FULL, ConfigUtils.toInternalName(sync.Receive.MethodCall.owner), sync.Receive.MethodCall.name,
                            sync.Receive.MethodCall.descriptor, sendIndices, sync.Receive.MethodCall.shouldReturnTrue);
                } else {
                    hbContract.addVertex(SynchronizationPointType.SEND, ConfigUtils.toInternalName(sync.Send.MethodCall.owner), sync.Send.MethodCall.name,
                            sync.Send.MethodCall.descriptor, sendIndices, sync.Send.MethodCall.shouldReturnTrue);
                    hbContract.addVertex(SynchronizationPointType.RECEIVE, ConfigUtils.toInternalName(sync.Receive.MethodCall.owner), sync.Receive.MethodCall.name,
                            sync.Receive.MethodCall.descriptor, receiveIndices, sync.Receive.MethodCall.shouldReturnTrue);
                }
                hbContracts.add(hbContract);
            }
            for (MultiSync sync : config.multiSyncs) {
                final String owner = ConfigUtils.toInternalName(sync.owner);
                List<Integer> argIndices = new ArrayList<Integer>();
                for (MultiLink link : sync.links) {
                    switch (link.type) {
                        case param:
                            argIndices.add(Integer.valueOf(link.args));
                            break;
                        case owner:
                            argIndices.add(-1);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown link type : " + link.type.toString());
                    }
                }
                HBContract hbContract = new HBContract();
                for (Call call : sync.calls) {
                    hbContract.addVertex(call.type, owner, call.name, call.descriptor, argIndices, call.shouldReturnTrue);
                }
                hbContracts.add(hbContract);
            }
            StringBuilder sb = new StringBuilder("\nHappens-before config read successfully.");
            if (hbContracts.size() <= 0) sb.append(" No happens-before contracts found.\n");
            else {
                sb.append(" Happens-before contracts are: \n\n");
                for (HBContract hbContract : hbContracts) {
                    sb.append(hbContract).append("\n");
                }
            }
            DRDLogger.log(sb.toString());
            return hbContracts;
        } catch (XStreamException e) {
            DRDLogger.error("Failed to parse drd config file '" + CONFIG_FILE + "'.", e);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //skip
                }
            }
        }
        return new ArrayList<HBContract>();
    }

    @XStreamAlias("HappensBeforeConfig")
    private static class HappensBeforeConfig {
        @XStreamAlias("Syncs")
        List<Sync> syncs;
        @XStreamAlias("Multiple-Syncs")
        List<MultiSync> multiSyncs;
    }

    @XStreamAlias("Multiple-Sync")
    private class MultiSync {
        @XStreamAsAttribute
        public String owner;
        @XStreamAlias("Multiple-Links")
        public List<MultiLink> links;
        @XStreamImplicit
        public List<Call> calls;
    }

    @XStreamAlias("Multiple-Link")
    private class MultiLink {
        @XStreamAsAttribute
        @XStreamConverter(EnumCaseInsensitiveConverter.class)
        public SyncType type;
        @XStreamAsAttribute
        public String args;
    }

    @XStreamAlias("Call")
    private class Call {
        @XStreamConverter(EnumCaseInsensitiveConverter.class)
        @XStreamAsAttribute
        public SynchronizationPointType type;
        @XStreamAsAttribute
        public String name;
        @XStreamAsAttribute
        public String descriptor;
        @XStreamAsAttribute
        @XStreamConverter(BooleanConverter.class)
        public boolean shouldReturnTrue;
    }

    @XStreamAlias("Sync")
    private static class Sync {
        @XStreamAlias("Links")
        List<Link> links;
        @XStreamAlias("Send")
        Send Send;
        @XStreamAlias("Receive")
        Receive Receive;
    }

    @XStreamAlias("Send")
    private static class Send {
        MethodCall MethodCall;
    }

    @XStreamAlias("Receive")
    private static class Receive {
        MethodCall MethodCall;
    }

    @XStreamAlias("MethodCall")
    private static class MethodCall {
        @XStreamAsAttribute
        public String owner;
        @XStreamAsAttribute
        public String name;
        @XStreamAsAttribute
        public String descriptor;
        @XStreamAsAttribute
        @XStreamConverter(BooleanConverter.class)
        public boolean shouldReturnTrue;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodCall that = (MethodCall) o;

            if (shouldReturnTrue != that.shouldReturnTrue) return false;
            if (descriptor != null ? !descriptor.equalsIgnoreCase(that.descriptor) : that.descriptor != null) return false;
            if (name != null ? !name.equalsIgnoreCase(that.name) : that.name != null) return false;
            if (owner != null ? !owner.equalsIgnoreCase(that.owner) : that.owner != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
            result = 31 * result + (shouldReturnTrue ? 1 : 0);
            return result;
        }
    }

    @XStreamAlias("Link")
    private static class Link {
        @XStreamAsAttribute
        @XStreamConverter(EnumCaseInsensitiveConverter.class)
        SyncType send;
        @XStreamAsAttribute
        @XStreamAlias("send-number")
        @XStreamConverter(IntConverter.class)
        int sendNumber;
        @XStreamAsAttribute
        @XStreamConverter(EnumCaseInsensitiveConverter.class)
        SyncType receive;
        @XStreamAsAttribute
        @XStreamAlias("receive-number")
        @XStreamConverter(IntConverter.class)
        int receiveNumber;
    }

    @XStreamAlias("type")
    private enum SyncType {
        param, owner
    }
}
