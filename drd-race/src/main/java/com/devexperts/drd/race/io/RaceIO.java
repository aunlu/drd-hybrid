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

package com.devexperts.drd.race.io;

import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.RaceCallback;
import com.devexperts.drd.race.impl.RaceImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Races serialization/deserialization to/from file.
 */
@SuppressWarnings("unused")
public class RaceIO {
    public interface Handle {
        /**
         * Stop listening for new races in associated file
         */
        void deactivate();
    }

    private static final JAXBContext CONTEXT;
    private static final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    static {
        try {
            CONTEXT = JAXBContext.newInstance(RaceImpl.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads all races from file
     *
     * @param file file
     * @return list of races
     * @throws RaceIOException if IO error occurs
     */
    public static List<Race> read(File file) throws RaceIOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return read(in);
        } catch (FileNotFoundException e) {
            throw new RaceIOException("File not found: " + file, e);
        } finally {
            Utils.closeQuietly(in);
        }
    }

    /**
     * Reads all races from file
     *
     * @param file path to file
     * @return list of races
     * @throws RaceIOException if IO error occurs
     */
    public static List<Race> read(String file) throws RaceIOException {
        return read(new File(file));
    }

    /**
     * Reads all races from stream
     *
     * @param in stream
     * @return list of races
     * @throws RaceIOException if IO error occurs
     */
    public static List<Race> read(final InputStream in) throws RaceIOException {
        try {
            return Utils.readRaces(in, createUnmarshaller());
        } catch (JAXBException e) {
            throw new RaceIOException("Race record corrupted", e);
        } catch (IOException e) {
            throw new RaceIOException("Race record corrupted", e);
        }
    }

    /**
     * Starts to listen for the changes in specified file with races and reports changes via callback
     *
     * @param file     file with races
     * @param callback callback
     * @return Handle that should be used to stop listening changes
     */
    public static Handle open(String file, RaceCallback callback) {
        WatchTack task = new WatchTack(file, callback);
        executor.submit(task);
        return task;
    }

    /**
     * Writes races to stream. Further they might be read using {@link #read(InputStream)} method.
     *
     * @param race race
     * @param out  stream
     * @throws RaceIOException if IO error occurs
     */
    public static void write(Race race, OutputStream out) throws RaceIOException {
        try {
            Marshaller m = CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.marshal(race, out);
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new RaceIOException("Failed to write race", e);
        }
    }

    private static Unmarshaller createUnmarshaller() {
        try {
            return CONTEXT.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Fatal JAXB error", e);
        }
    }

    private static final class WatchTack implements Runnable, Handle {
        private final RaceCallback callback;
        private final Path file;
        private final RaceReader reader;
        private volatile boolean active = true;

        public WatchTack(String file, RaceCallback callback) {
            this.file = Paths.get(file);
            this.callback = callback;
            this.reader = new RaceReader(file, createUnmarshaller());
        }

        @Override
        public void run() {
            WatchService watchService = null;
            try {
                watchService = FileSystems.getDefault().newWatchService();
                file.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
                if (Files.exists(file)) {
                    readData(); //read initial data, if present
                }
                while (active) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                        List<WatchEvent<?>> watchEvents = key.pollEvents();
                        for (WatchEvent event : watchEvents) {
                            if (file.getFileName().equals(event.context())) {
                                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    readData();
                                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                    callback.deleted();
                                } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                    callback.created();
                                }
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            callback.error(new RaceIOException("Directory has been deleted: " + file.getParent()));
                        }
                    } catch (InterruptedException e) {
                        //skip
                    }
                }
            } catch (IOException e) {
                callback.error(new RaceIOException("Failed to listen file changes: " + file, e));
            } finally {
                Utils.closeQuietly(watchService);
            }
        }

        private void readData() {
            try {
                callback.newData(reader.read());
            } catch (IOException e) {
                callback.error(new RaceIOException("Failed to read races from file " + file, e));
            } catch (JAXBException e) {
                callback.error(new RaceIOException("Failed to unmarshall races from file " + file, e));
            }
        }

        @Override
        public void deactivate() {
            active = false;
        }
    }
}
