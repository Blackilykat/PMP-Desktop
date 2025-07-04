/*
 * Copyright (C) 2025 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.widgets.filters;

import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.widgets.Widget;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryFiltersWidget extends Widget {
    private List<LibraryFilterPanel> panels = new ArrayList<>();
    public final Audio audio;
    public JPanel container = new JPanel();

    public LibraryFiltersWidget(Audio audio) {
        this.audio = audio;

        this.add(container);

        layout.putConstraint(SpringLayout.NORTH, container, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, container, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, container, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.WEST, container, 0, SpringLayout.WEST, this);

        JPopupMenu popup = new JPopupMenu();
        popup.add(this.getAddFilterPopupItem());

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    popup.show(LibraryFiltersWidget.this, e.getX(), e.getY());
                }
            }
        });

        reloadPanels();
        reloadElements();
    }

    public void reloadPanels() {
        panels.clear();
        if(audio.currentSession == null) return;

        for(LibraryFilter filter : audio.currentSession.getLibraryFilters()) {
            LibraryFilterPanel panel = new LibraryFilterPanel(filter, this);
            panel.reloadOptions();
            panels.add(panel);
        }
    }

    public void reloadElements() {
        container.removeAll();
        container.setLayout(new GridBagLayout());
        GridBagConstraints constraints;
        for(LibraryFilterPanel panel : panels) {
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = GridBagConstraints.RELATIVE;
            constraints.weighty = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.BOTH;
            container.add(panel, constraints);
        }
        updateUI();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, 100);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public JMenuItem getAddFilterPopupItem() {
        JMenuItem item = new JMenuItem("Add Filter");
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String answer = JOptionPane.showInputDialog("Insert the metadata to filter through");
                LibraryFilter filter = new LibraryFilter(Library.INSTANCE, audio.currentSession, answer);
                filter.reloadOptions();
                if(Audio.INSTANCE != null && Audio.INSTANCE.currentSession != null) {
                    Audio.INSTANCE.currentSession.addLibraryFilter(filter);
                }
                LibraryFilterPanel panel = new LibraryFilterPanel(filter, LibraryFiltersWidget.this);
                panel.filter.library.reloadFilters();
                reloadPanels();
                reloadElements();
            }
        });
        return item;
    }
}
