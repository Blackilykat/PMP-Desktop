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

package dev.blackilykat.widgets;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Utility panel that will always have its preferred size as:<br />
 * Width: width of the parent<br />
 * Height: sum of the children's heights<br />
 * Designed to be used with {@link javax.swing.JScrollPane}
 */
public class ScrollablePanel extends JPanel {

    public ScrollablePanel() {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Dimension getPreferredSize() {
        int height = 0;
        for (Component component : this.getComponents()) {
            height += component.getHeight();
        }
        return new Dimension(this.getParent().getWidth(), height);
    }
}
