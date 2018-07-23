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

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;

public class RaceTableModel extends AbstractTableModel {
    private List<Race> races = Collections.emptyList();

    public void setRaces(List<Race> races) {
        this.races = races;
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Time";
            case 1:
                return "Target";
            case 2:
                return "Current thread";
            case 3:
                return "Racing thread";
            default:
                throw new IllegalArgumentException("Unexpected column: " + column);
        }
    }

    public int getRowCount() {
        return races.size();
    }

    public int getColumnCount() {
        return 4;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        final Race race = getRace(rowIndex);
        switch (columnIndex) {
            case 0:
                return Util.format(race.getTime());
            case 1:
                return Util.getTarget(race);
            case 2:
                return Util.getDescription(race.getCurrentAccess(), race.getRaceTargetType(), false);
            case 3:
                return Util.getDescription(race.getRacingAccess(), race.getRaceTargetType(), false);
            default:
                throw new IllegalArgumentException("Unexpected column: " + columnIndex);
        }
    }

    Race getRace(int rowIndex) {
        return races.get(rowIndex);
    }

    List<Race> getRaces() {
        return Collections.unmodifiableList(races);
    }
}