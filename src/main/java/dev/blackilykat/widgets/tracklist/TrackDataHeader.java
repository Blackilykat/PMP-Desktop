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
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class TrackDataHeader {
    public final String name;
    public final String metadataKey;
    public final Class<? extends TrackDataEntry<?>> clazz;
    public int width;
    public JPanel containedComponent = null;
    private JLabel component = null;
    public SongListWidget songListWidget;
    private TrackDataEntry.Alignment alignment = null;

    public TrackDataHeader(String name, String metadataKey, Class<? extends TrackDataEntry<?>> clazz, int width, SongListWidget songListWidget) {
        this.name = name;
        this.metadataKey = metadataKey;
        this.clazz = clazz;
        this.width = width;
        this.songListWidget = songListWidget;
    }

    public JComponent getComponent() {
        if(component != null) return component;
        component = new JLabel(name);
        component.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        return component;
    }

    public JPanel getContainedComponent() {
        if(containedComponent == null) {
            containedComponent = new JPanel();
            containedComponent.setLayout(new BoxLayout(containedComponent, BoxLayout.X_AXIS));
            containedComponent.add(getComponent());
        }

        Dimension size = new Dimension(width, 32);
        containedComponent.setMinimumSize(size);
        containedComponent.setPreferredSize(size);
        containedComponent.setMaximumSize(size);

        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("CHILDREN: " + Arrays.toString(containedComponent.getComponents()));
                if(songListWidget.draggedHeader == null) return;
                songListWidget.draggedHeader.width = Math.max(e.getX() + containedComponent.getX() - songListWidget.draggedHeader.containedComponent.getX(), 20);

                songListWidget.draggedHeader = null;
                songListWidget.dragResizeLine = -1;
                songListWidget.repaint();
                songListWidget.refresh();
                songListWidget.revalidate();
                songListWidget.scrollPaneContents.revalidate();
                for(Component child : songListWidget.scrollPaneContents.getComponents()) {
                    if(!(child instanceof TrackPanel trackPanel)) continue;
                    for(Component grandchild : trackPanel.getComponents()) {
                        grandchild.revalidate();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(songListWidget.draggedHeader == null) return;
                System.out.println(containedComponent.getX() + ", " + e.getX());

                int actualX = e.getX() + containedComponent.getX() - songListWidget.draggedHeader.containedComponent.getX();
                int oldPos = songListWidget.dragResizeLine;
                if(actualX < 20) {
                    // can safely assume other headers' containedComponents are non-null since the user is dragging on this one
                    songListWidget.dragResizeLine = songListWidget.draggedHeader.containedComponent.getX() + 20;
                } else {
                    songListWidget.dragResizeLine = containedComponent.getX() + e.getX();
                }
                // repainting everything uses an unreasonable amount of gpu
                songListWidget.repaint(oldPos, 0, 1, songListWidget.getHeight());
                songListWidget.repaint(songListWidget.dragResizeLine, 0, 1, songListWidget.getHeight());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getX() < 20) {
                    int index = songListWidget.dataHeaders.indexOf(TrackDataHeader.this) - 1;
                    if(index >= 0) {
                        songListWidget.draggedHeader = songListWidget.dataHeaders.get(index);
                    } else {
                        songListWidget.draggedHeader = null;
                    }
                } else if(e.getX() > width - 20) {
                    songListWidget.draggedHeader = TrackDataHeader.this;
                }
            }
        };

        containedComponent.addMouseListener(listener);
        containedComponent.addMouseMotionListener(listener);

        return containedComponent;
    }

    //TODO this is a mess, please clean it up with #8
    public void setAlignment(TrackDataEntry.Alignment alignment) {
        this.alignment = alignment;
        containedComponent.removeAll();
        switch(alignment) {
            case LEFT -> {
                containedComponent.add(component);
                containedComponent.add(Box.createHorizontalGlue());
            }
            case CENTER -> {
                containedComponent.add(component);
            }
            case RIGHT -> {
                containedComponent.add(Box.createHorizontalGlue());
                containedComponent.add(component);
            }
        }
        Dimension size = new Dimension(width, 32);
        containedComponent.setMinimumSize(size);
        containedComponent.setPreferredSize(size);
        containedComponent.setMaximumSize(size);
    }

    public TrackDataEntry.Alignment getAlignment() {
        return this.alignment;
    }

}
