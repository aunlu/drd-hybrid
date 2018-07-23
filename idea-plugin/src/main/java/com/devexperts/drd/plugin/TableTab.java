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
import com.devexperts.drd.race.RaceCallback;
import com.devexperts.drd.race.io.RaceIO;
import com.devexperts.drd.race.io.RaceIOException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.util.List;

class TableTab {
    private JTable table;
    private DRDTableModel model;
    private RaceIO.Handle handle;
    private JPanel component;
    private Project project;
    private File file;
    private DialogTable dialogTable;
    private JTabbedPane parent;

    TableTab(Project project, File file, DialogTable dialogTable) {
        this.project = project;
        this.file = file;
        this.dialogTable = dialogTable;
        this.parent = dialogTable.getTabbedPane();
    }


    JPanel createTable() {
        model = new DRDTableModel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(createCloseTabButtonActionListener());
        JScrollPane pane;
        table = new DRDTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(170);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        addPopupMenu(table);
        pane = new JBScrollPane(table);
        createHandler();
        JPanel panel = new JPanel(new VerticalLayout(5));
        panel.add(closeButton);
        panel.add(pane);
        component = panel;
        return panel;
    }

    private void createHandler() {
        TableTab tableTab = this;

        RaceCallback callback = new RaceCallback() {
            @Override
            public void newData(List<Race> races) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String info = "New data arrived to file \"" + (file.getName()) + "\" in tab #" + (parent.indexOfComponent(component) + 1);
                        Notification notification = new Notification("Data Race Detector", "DRD", info, NotificationType.INFORMATION);
                        model.changeData(races);
                        table.updateUI();
                        notification.notify(project);
                    }
                });
            }

            @Override
            public void deleted() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String info = "File \"" + (file.getName()) + "\" with logs was deleted in tab #" + (parent.indexOfComponent(component) + 1);
                        Notification notification = new Notification("Data Race Detector", "DRD", info, NotificationType.WARNING);
                        model.clear();
                        table.updateUI();
                        notification.notify(project);
                    }
                });
            }

            @Override
            public void created() {
                String info = "File \"" + (file.getName()) + "\" with logs was created in tab #" + (parent.indexOfComponent(component) + 1);
                Notification notification = new Notification("Data Race Detector", "DRD", info, NotificationType.INFORMATION);
                notification.notify(project);
            }

            @Override
            public void error(RaceIOException e) {
                String info = "Error in tab #" + (parent.indexOfComponent(component) + 1) + "!\n" + e.getMessage();
                Notification notification = new Notification("Data Race Detector", "DRD", info, NotificationType.ERROR);
                handle.deactivate();
                parent.remove(component);
                notification.notify(project);
                dialogTable.removeDeactivatedTab(tableTab);
            }
        };

        handle = RaceIO.open(file.getAbsolutePath(), callback);
    }

    private ActionListener createCloseTabButtonActionListener() {
        TableTab tableTab = this;

        return e -> {
            if (parent.getSelectedIndex() != 0) {
                parent.setSelectedIndex(parent.getSelectedIndex() - 1);
            }

            handle.deactivate();
            parent.remove(component);
            dialogTable.removeDeactivatedTab(tableTab);
        };
    }

    private void addPopupMenu(JTable table) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setInvoker(table);
        popupMenu.add(createOption("Copy current access stacktrace to clipboard", 0));
        popupMenu.add(createOption("Copy racing access stacktrace to clipboard", 1));
        table.addMouseListener(createPopupMouseAdapter(popupMenu));
    }

    private AbstractAction createOption(String option, int key) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();

                if (row != -1) {
                    Race race = ((DRDTableModel) table.getModel()).getRace(row);
                    copyStackTraceToClipboard(TableUtils.getFullStackTrace(
                            (key == 0 ? race.getCurrentAccess() : race.getRacingAccess())));
                }
            }

            @Override
            public Object getValue(String key) {
                return key.equals(Action.NAME) ? option : super.getValue(key);
            }
        };
    }

    private void copyStackTraceToClipboard(String stackTrace) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(stackTrace), null);
    }

    private MouseListener createPopupMouseAdapter(JPopupMenu popupMenu) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());

                if (row != -1) {
                    table.getSelectionModel().setSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
    }

    RaceIO.Handle getHandle() {
        return handle;
    }
}
