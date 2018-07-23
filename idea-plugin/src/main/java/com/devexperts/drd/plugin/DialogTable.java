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

import com.intellij.notification.EventLog;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

class DialogTable {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private final Set<TableTab> tableTabs = new HashSet<>();
    private Project project;

    DialogTable(Project project) {
        this.project = project;
    }

    void createAndShowUI() {
        frame = firstInit();
        new EventLog();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tableTabs.forEach(tableTab -> tableTab.getHandle().deactivate());
                frame.dispose();
            }
        });
    }

    private JFrame firstInit() {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        Border border = new LineBorder(JBColor.WHITE);
        panel.setBorder(border);
        tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("+", new JBScrollPane());
        tabbedPane.addMouseListener(createTabMouseListener(panel));
        panel.add(tabbedPane);
        frame.add(panel);
        frame.pack();
        frame.setSize(new Dimension(600, 400));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        return frame;
    }

    private File getLogFileFromFileChooser(JPanel panel) {
        JFileChooser fileChooser = new JFileChooser(project.getBasePath());
        File file = null;
        int returnVal = fileChooser.showOpenDialog(panel);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        }

        return file;
    }

    private MouseListener createTabMouseListener(JPanel panel) {
        DialogTable dialogTable = this;

        return new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int tabNumber = tabbedPane.indexAtLocation(e.getX(), e.getY());

                if (tabNumber == tabbedPane.getTabCount() - 1) {
                    File file = getLogFileFromFileChooser(panel);

                    if (file == null) {
                        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                    } else {
                        TableTab tableTab = new TableTab(project, file, dialogTable);
                        tabbedPane.insertTab(file.getName(), null, tableTab.createTable(), null, tabbedPane.getTabCount() - 1);
                        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                        tableTabs.add(tableTab);
                    }
                }
            }
        };
    }

    JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    void removeDeactivatedTab(TableTab tableTab) {
        tableTabs.remove(tableTab);
    }
}
