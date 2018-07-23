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

package com.devexperts.drd.gui;

import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.io.RaceIO;
import com.devexperts.drd.race.io.RaceIOException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Tab {
    private File file;
    private RaceTableModel tableModel;

    public Tab(File file, RaceTableModel tableModel) throws IOException {
        this.file = file;
        this.tableModel = tableModel;
        reload();
    }

    void reload() {
        List<Race> races;
        try {
            races = RaceIO.read(file);
        } catch (RaceIOException e) {
            e.printStackTrace();
            races = Collections.emptyList();
        }
        //do not repaint if no new races
        if (tableModel.getRowCount() != races.size()) {
            updateTableModel(races);
        }
    }

    private void updateTableModel(final List<Race> races) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                tableModel.setRaces(races);
            }
        });
    }
}