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

package com.devexperts.drd.plugin;

import com.devexperts.drd.race.Race;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

class DRDTableModel extends AbstractTableModel {

    private List<Race> races;

    DRDTableModel() {
        this.races = new ArrayList<>();
    }

    void changeData(List<Race> races) {
        this.races.addAll(races);
    }

    void clear() {
        this.races.clear();
    }

    public int getColumnCount() {
        return 4;
    }

    Race getRace(int row) {
        return races.get(row);
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Time";
            case 1:
                return "Race Target Type";
            case 2:
                return "Current Access";
            case 3:
                return "Racing Access";
        }

        return null;
    }

    @Override
    public int getRowCount() {
        return races.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Race race = races.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return TableUtils.convertDateToReadable(race.getTime());
            case 1:
                return TableUtils.convertRaceTargetTypeToReadable(race);
            case 2:
                return TableUtils.convertAccessToReadable(race, race.getCurrentAccess());
            case 3:
                return TableUtils.convertAccessToReadable(race, race.getRacingAccess());
        }

        return null;
    }
}