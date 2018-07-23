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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all existing tabs and forces them to reload once in two seconds
 */
public class TabManager {
    private final List<Tab> tabs = new CopyOnWriteArrayList<Tab>();

    public TabManager() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Tab tab : tabs) {
                    tab.reload();
                }
            }
        }, 0L, 5000L);
    }

    public RaceTableModel addTab(File file) throws IOException {
        RaceTableModel tableModel = new RaceTableModel();
        tabs.add(new Tab(file, tableModel));
        return tableModel;
    }
}
