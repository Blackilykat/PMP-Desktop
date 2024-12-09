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

package dev.blackilykat.widgets.tracklist;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class TrackDataEntryWidget extends JPanel {
    private TrackDataHeader<?> dataHeader;
    private TrackPanel panel;

    public TrackDataEntryWidget(TrackDataEntry<?> entry, TrackDataHeader<?> dataHeader, TrackPanel panel) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.dataHeader = dataHeader;
        this.panel = panel;
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        setMinimumSize(getPreferredSize());
        setPreferredSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        this.setSize(getPreferredSize());

        if(entry != null) {
            JComponent component = entry.getComponent();
            if(component != null) {
                if(entry.getAlignment() == TrackDataEntry.Alignment.RIGHT) {
                    this.add(Box.createHorizontalGlue());
                }
                component.setAlignmentY(JComponent.CENTER_ALIGNMENT);
                this.add(component);
                if(entry.getAlignment() == TrackDataEntry.Alignment.LEFT) {
                    this.add(Box.createHorizontalGlue());
                }
            }
        }

        // layout ignores minimum size if this is not there. no clue why
        if(this.getComponents().length == 0) {
            JPanel alignmentPanel = new JPanel();
            Dimension size = new Dimension(0, getPreferredSize().height + 1);
            alignmentPanel.setMinimumSize(size);
            alignmentPanel.setPreferredSize(size);
            alignmentPanel.setMaximumSize(size);
            alignmentPanel.setSize(size);
            this.add(alignmentPanel);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(dataHeader.width, 30);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(panel.getParent().getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(panel.getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        if(panel.getMouseHovering()) {
            g.setColor(new Color(0, 0, 0, 20));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }
}
