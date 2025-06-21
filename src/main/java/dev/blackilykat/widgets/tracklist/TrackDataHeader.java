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

package dev.blackilykat.widgets.tracklist;

import dev.blackilykat.Library;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TrackDataHeader {
    public static int latestId = -1;
    public String metadataKey;
    public Class<? extends TrackDataEntry<?>> clazz;
    public String name;
    public int width;
    public final int id;
    public JPanel containedComponent = null;
    private JLabel component = null;
    public SongListWidget songListWidget;
    private TrackDataEntry.Alignment alignment = null;
    private int lastPressedX = -1;
    private boolean wasPressing = false;

    public TrackDataHeader(String name, String metadataKey, Class<? extends TrackDataEntry<?>> clazz, int width, SongListWidget songListWidget) {
        this(latestId++, name, metadataKey, clazz, width, songListWidget);
    }

    public TrackDataHeader(int id, String name, String metadataKey, Class<? extends TrackDataEntry<?>> clazz, int width, SongListWidget songListWidget) {
        this.id = id;
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
            containedComponent = new Container(this);
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
                // for some reason mouseReleased and mousePressed get called multiple times when you drag your mouse.
                // it makes absolutely no sense for it to do that but this fixes it
                if(!wasPressing) return;
                wasPressing = false;

                if(songListWidget.draggedHeader == null) {
                    if(songListWidget.audio.currentSession.getSortingHeader() != TrackDataHeader.this) {
                        songListWidget.audio.currentSession.setSortingHeader(TrackDataHeader.this);
                        songListWidget.audio.currentSession.setSortingOrder(Order.DESCENDING);
                    } else {
                        // a lil confusing to read but it just flips it
                        songListWidget.audio.currentSession.setSortingOrder(songListWidget.audio.currentSession.getSortingOrder() == Order.DESCENDING ? Order.ASCENDING : Order.DESCENDING);
                    }
                    lastPressedX = -1;
                    // repainting the headers alone clears the lines between them, so you have to redraw the whole thing
                    // (you could define the specific regions to redraw but i dont feel like doing that)
                    songListWidget.headerPanel.repaint();

                    Library.INSTANCE.reloadSorting();
                    return;
                }
                songListWidget.draggedHeader.width = Math.max(e.getX() + containedComponent.getX() - songListWidget.draggedHeader.containedComponent.getX(), 20);

                songListWidget.draggedHeader = null;
                songListWidget.dragResizeLine = -1;
                songListWidget.repaint();
                songListWidget.refreshHeaders();
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
                if(wasPressing) return;
                wasPressing = true;
                lastPressedX = e.getX();
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

    private static class Container extends JPanel {
        public final TrackDataHeader parent;
        public int lastKnownWidth;
        public int[] arrowCoordinatesX = new int[3];
        public int[] ascendingArrowCoordinatesY = new int[3];
        public int[] descendingArrowCoordinatesY = new int[3];

        public Container(TrackDataHeader parent) {
            this.parent = parent;
            updateCoordinates();
        }

        private void updateCoordinates() {
            lastKnownWidth = parent.width;
            arrowCoordinatesX[0] = lastKnownWidth / 2;
            arrowCoordinatesX[1] = arrowCoordinatesX[0] - 5;
            arrowCoordinatesX[2] = arrowCoordinatesX[0] + 5;
            ascendingArrowCoordinatesY[0] = 2;
            ascendingArrowCoordinatesY[1] = ascendingArrowCoordinatesY[2] = 2 + 7;
            descendingArrowCoordinatesY[0] = 30;
            descendingArrowCoordinatesY[1] = descendingArrowCoordinatesY[2] = 30 - 7;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if(lastKnownWidth != parent.width) {
                updateCoordinates();
            }
            if(parent.songListWidget.audio.currentSession.getSortingHeader() == parent) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.GRAY);
                if(parent.songListWidget.audio.currentSession.getSortingOrder() == Order.ASCENDING) {
                    g.fillPolygon(arrowCoordinatesX, ascendingArrowCoordinatesY, 3);
                } else {
                    g.fillPolygon(arrowCoordinatesX, descendingArrowCoordinatesY, 3);
                }
            }
        }
    }

}
