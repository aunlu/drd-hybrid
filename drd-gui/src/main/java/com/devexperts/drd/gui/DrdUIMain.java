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

import com.devexperts.drd.race.Access;
import com.devexperts.drd.race.Race;
import com.devexperts.drd.race.RaceTargetType;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Launches DRD UI
 */
public class DrdUIMain {
    private static final TabManager tabManager = new TabManager();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowUI();
            }
        });
    }

    private static void createAndShowUI() {
        setLookAndFeel();
        final JTabbedPane tabbedPane = new JTabbedPane();
        final JPanel initialPanel = new JPanel();
        final JFrame mainFrame = new JFrame();
        initialPanel.add(new JLabel("Load drd file. Results would be displayed on new tab"));
        JButton loadButton = new JButton("Load file ...");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(initialPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        final JTable table = new RaceTable(tabManager.addTab(file));
                        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
                        table.setRowSorter(sorter);
                        configureTable(table);
                        JScrollPane scrollPane = new JScrollPane(table);
                        JPanel panel = new JPanel();
                        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                        panel.add(scrollPane);
                        JPanel buttonsPanel = new JPanel();
                        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
                        JButton printButton = new JButton(new AbstractAction() {
                            @Override
                            public Object getValue(String key) {
                                return key.equals(Action.NAME) ? "Print config change" : super.getValue(key);
                            }

                            public void actionPerformed(ActionEvent e) {
                                RaceTableModel model = (RaceTableModel) table.getModel();
                                System.out.println(new ConfigUpdater().getConfigChange(model.getRaces()));
                            }
                        });
                        printButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                        buttonsPanel.add(printButton);
                        JButton showUniqueButton = new JButton(new AbstractAction() {
                            @Override
                            public Object getValue(String key) {
                                return key.equals(Action.NAME) ? "Show unique races" : super.getValue(key);
                            }

                            public void actionPerformed(ActionEvent e) {
                                RaceTableModel tableModel = (RaceTableModel) table.getModel();
                                JDialog dialog = new JDialog((JFrame) null, "Unique targets", true);
                                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                                JTextArea textArea = new JTextArea(sortRaces(tableModel.getRaces()));
                                Font font = textArea.getFont();
                                float size = 12;
                                textArea.setFont(font.deriveFont(size));
                                JScrollPane sp = new JScrollPane(textArea);
                                dialog.add(sp);
                                dialog.setPreferredSize(new Dimension(1000, 600));
                                dialog.setMaximumSize(new Dimension(1000, 600));
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                            }
                        });
                        showUniqueButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                        buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
                        buttonsPanel.add(showUniqueButton);
                        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        panel.add(Box.createRigidArea(new Dimension(0, 20)));
                        panel.add(buttonsPanel);
                        tabbedPane.addTab(file.getAbsolutePath(), panel);
                        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        initialPanel.add(loadButton);
        tabbedPane.addTab("Load file", initialPanel);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.getContentPane().add(tabbedPane);
        mainFrame.setPreferredSize(new Dimension(600, 400));
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private static void copyToClipboard(String s) {
        final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable transferableText = new StringSelection(s);
        systemClipboard.setContents(transferableText, null);
    }

    private static void configureTable(final JTable table) {
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setInvoker(table);
        popupMenu.add(new AbstractAction() {
            @Override
            public Object getValue(String key) {
                return key.equals(Action.NAME) ? "Copy both stacktraces to clipboard" : super.getValue(key);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    copyToClipboard("Current stack trace: \n\n" +
                            Util.getRawStackTrace(((RaceTableModel) table.getModel()).getRace(selectedRow).getCurrentAccess()) +
                            "\n\nRacing stack trace: \n\n" +
                            Util.getRawStackTrace(((RaceTableModel) table.getModel()).getRace(selectedRow).getCurrentAccess()));
                }
            }
        });
/*        popupMenu.add(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    copyToClipboard(Util.getRawStackTrace(((RaceTableModel) table.getModel()).getRace(selectedRow).getCurrentAccess().getStackTrace()));
                }
            }

            @Override
            public Object getValue(String key) {
                return key.equals(Action.NAME) ? "Copy current stacktrace to clipboard" : super.getValue(key);
            }
        });
        popupMenu.add(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    copyToClipboard(Util.getRawStackTrace(((RaceTableModel) table.getModel()).getRace(selectedRow).getRacingAccess().getStackTrace()));
                }
            }

            @Override
            public Object getValue(String key) {
                return key.equals(Action.NAME) ? "Copy racing stacktrace to clipboard" : super.getValue(key);
            }
        });*/
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    displayPopupMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    displayPopupMenu(e);
                }
            }

            private void displayPopupMenu(MouseEvent e) {
                if (table.isEditing()) {
                    table.removeEditor();
                }
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                if (row < 0) {
                    return;
                }
                table.getSelectionModel().setSelectionInterval(row, row);
                popupMenu.show(e.getComponent(), point.x, point.y);
            }

        });
        table.getColumnModel().getColumn(0).setMaxWidth(160);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
    }

    private static String sortRaces(List<Race> races) {
        SortedMap<String, Integer> fieldRaces = new TreeMap<String, Integer>();
        SortedMap<String, Integer> objectRaces = new TreeMap<String, Integer>();
        for (Race race : races) {
            String target;
            Map<String, Integer> map;
            Access access = race.getCurrentAccess();
            if (race.getRaceTargetType() == RaceTargetType.OBJECT) {
                target = access.getTargetInfo().get(Access.OBJECT_TYPE);
                map = objectRaces;
            } else {
                target = access.getTargetInfo().get(Access.FIELD_OWNER);
                map = fieldRaces;
            }
            Integer value = map.get(target);
            if (value == null) {
                map.put(target, 1);
            } else {
                map.put(target, value + 1);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("---------Races on object calls:----------\n");
        for (Map.Entry<Integer, List<String>> entry : invert(objectRaces).entrySet()) {
            for (String desc : entry.getValue()) {
                sb.append(desc).append(" : ").append(entry.getKey()).append(" races\n");
            }
        }
        sb.append("\n\n");
        sb.append("---------Races on field accesses in classes:----------\n");
        for (Map.Entry<Integer, List<String>> entry : invert(fieldRaces).entrySet()) {
            for (String desc : entry.getValue()) {
                sb.append(desc).append(" : ").append(entry.getKey()).append(" races\n");
            }
        }
        return sb.toString();
    }

    private static <T> SortedMap<Integer, List<T>> invert(SortedMap<T, Integer> map) {
        SortedMap<Integer, List<T>> inverted = new TreeMap<Integer, List<T>>(Collections.reverseOrder());
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            List<T> l = inverted.get(entry.getValue());
            if (l == null) {
                l = new ArrayList<T>();
                inverted.put(entry.getValue(), l);
            }
            l.add(entry.getKey());
        }
        return inverted;
    }
}
