/*
 * PMP-Desktop - A desktop client for Personal Music Platform, a
 * self-hosted platform to play music and make sure everything is
 * always synced across devices.
 * Copyright (C) 2024 Blackilykat
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

package dev.blackilykat.widgets.filters;

import dev.blackilykat.Library;
import dev.blackilykat.widgets.Widget;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

public class LibraryFiltersWidget extends Widget {
    public List<LibraryFilterPanel> panels = new ArrayList<>();
    public JPanel container = new JPanel();

    public LibraryFiltersWidget() {
        this.add(container);

        layout.putConstraint(SpringLayout.NORTH, container, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, container, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, container, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.WEST, container, 0, SpringLayout.WEST, this);

        panels.add(new LibraryFilterPanel(new LibraryFilter(Library.INSTANCE, "artist")));
        panels.add(new LibraryFilterPanel(new LibraryFilter(Library.INSTANCE, "album")));
        panels.add(new LibraryFilterPanel(new LibraryFilter(Library.INSTANCE, "instrumental")));

        reloadElements();
    }

    public void reloadElements() {
        container.removeAll();
        container.setLayout(new GridBagLayout());
        GridBagConstraints constraints;
        for(LibraryFilterPanel panel : panels) {
            System.out.println("Aaaaa");

            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = GridBagConstraints.RELATIVE;
            constraints.weighty = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.BOTH;
            container.add(panel, constraints);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, 100);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}
