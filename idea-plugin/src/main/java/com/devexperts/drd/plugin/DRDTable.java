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

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

class DRDTable extends JBTable {

    DRDTable(TableModel model) {
        super(model);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);

        if (component instanceof JComponent) {
            ((JComponent) component).setToolTipText(getToolTipText(row, column));
        }

        return component;
    }

    private String getToolTipText(int row, int column) {
        switch (column) {
            case 2:
                return TableUtils.getTooltipStackTrace(((DRDTableModel) dataModel)
                        .getRace(row).getCurrentAccess());
            case 3:
                return TableUtils.getTooltipStackTrace(((DRDTableModel) dataModel)
                        .getRace(row).getRacingAccess());
            default:
                return "";
        }
    }
}
