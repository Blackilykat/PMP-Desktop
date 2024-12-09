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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class TrackDataHeader<T extends TrackDataEntry<T>> {
    public final String name;
    public final String metadataKey;
    public final Class<T> clazz;
    public int width;
    private JPanel containedComponent = null;

    public TrackDataHeader(String name, String metadataKey, Class<T> clazz, int width) {
        this.name = name;
        this.metadataKey = metadataKey;
        this.clazz = clazz;
        this.width = width;
    }

    public JComponent getComponent() {
        return new JLabel(name);
    }

    public JPanel getContainedComponent() {
        if(containedComponent == null) {
            containedComponent = new JPanel();
        }

        Dimension size = new Dimension(width, 32);
        containedComponent.setMinimumSize(size);
        containedComponent.setPreferredSize(size);
        containedComponent.setMaximumSize(size);

        containedComponent.removeAll();
        containedComponent.add(getComponent());

        return containedComponent;
    }

    //TODO this is a mess, please clean it up with #8
    public void setAlignment(TrackDataEntry.Alignment alignment) {
        switch(alignment) {
            case LEFT -> containedComponent.setLayout(new FlowLayout(FlowLayout.LEFT));
            case CENTER -> containedComponent.setLayout(new FlowLayout(FlowLayout.CENTER));
            case RIGHT -> containedComponent.setLayout(new FlowLayout(FlowLayout.RIGHT));
        }
        Dimension size = new Dimension(width, 32);
        containedComponent.setMinimumSize(size);
        containedComponent.setPreferredSize(size);
        containedComponent.setMaximumSize(size);
    }

}
