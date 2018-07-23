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

import javax.swing.*;
import java.awt.event.MouseEvent;

public class RaceTable extends JTable {
    private RaceTableModel raceTableModel;

    public RaceTable(RaceTableModel raceTableModel) {
        super(raceTableModel);
        this.raceTableModel = raceTableModel;
    }

    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int colIndex = columnAtPoint(p);
        Race race = raceTableModel.getRace(getRowSorter().convertRowIndexToModel(rowAtPoint(p)));
        switch (colIndex) {
            case 0: return "";
            case 1: return Util.getTarget(race);
            case 2: return Util.getDescription(race.getCurrentAccess(), race.getRaceTargetType(), true);
            case 3: return Util.getDescription(race.getRacingAccess(), race.getRaceTargetType(), true);
            default: throw new IllegalArgumentException("Unexpected col index : "  + colIndex);
        }
    }
}