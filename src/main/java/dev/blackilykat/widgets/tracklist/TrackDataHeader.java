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
import dev.blackilykat.Main;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.DataHeaderListMessage;
import dev.blackilykat.messages.LatestHeaderIdMessage;
import dev.blackilykat.util.Triple;
import jnr.ffi.annotations.In;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;

import static dev.blackilykat.Main.LOGGER;

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
    private JPopupMenu popupMenu = new JPopupMenu();

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

        popupMenu.add(songListWidget.getAddHeaderPopupItem());
        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                songListWidget.dataHeaders.remove(TrackDataHeader.this);
                songListWidget.refreshHeaders();
                Main.songListWidget.refreshTracks();

                if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.loggedIn) {
                    DataHeaderListMessage msg = new DataHeaderListMessage();
                    for(TrackDataHeader header : songListWidget.dataHeaders) {
                        msg.headers.add(new Triple<>(header.id, header.metadataKey, header.name));
                    }
                    ServerConnection.INSTANCE.send(msg);
                    ServerConnection.INSTANCE.send(new LatestHeaderIdMessage(TrackDataHeader.latestId));
                }

            }
        });
        popupMenu.add(removeItem);
        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                JTextField labelField = new JTextField(15);
                labelField.setText(TrackDataHeader.this.name);
                JTextField keyField = new JTextField(15);
                keyField.setText(TrackDataHeader.this.metadataKey);
                int r = JOptionPane.showConfirmDialog(null,
                        new Object[]{"Label: ", labelField, "Key: ", keyField},
                        "Edit header",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if(r != JOptionPane.YES_OPTION) {
                    return;
                }

                TrackDataHeader.this.name = labelField.getText();
                TrackDataHeader.this.metadataKey = keyField.getText();
                if(TrackDataHeader.this.component != null) {
                    TrackDataHeader.this.component.setText(TrackDataHeader.this.name);
                }

                songListWidget.refreshHeaders();
                Main.songListWidget.refreshTracks();

                if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.loggedIn) {
                    DataHeaderListMessage msg = new DataHeaderListMessage();
                    for(TrackDataHeader header : songListWidget.dataHeaders) {
                        msg.headers.add(new Triple<>(header.id, header.metadataKey, header.name));
                    }
                    ServerConnection.INSTANCE.send(msg);
                    ServerConnection.INSTANCE.send(new LatestHeaderIdMessage(TrackDataHeader.latestId));
                }
            }
        });
        popupMenu.add(editItem);
    }

    public JLabel getComponent() {
        if(component != null) return component;
        component = new JLabel(name) {
            @Override
            public Color getForeground() {
                return (songListWidget.dragging && songListWidget.draggedHeader == TrackDataHeader.this) ?  Color.GRAY : super.getForeground();
            }
        };
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
                if(e.getButton() == MouseEvent.BUTTON1) {
                    // for some reason mouseReleased and mousePressed get called multiple times when you drag your mouse.
                    // it makes absolutely no sense for it to do that but this fixes it
                    if(!wasPressing) return;
                    wasPressing = false;

                    if(songListWidget.resizedHeader != null) {
                        songListWidget.resizedHeader.width = Math.max(e.getX() + containedComponent.getX() - songListWidget.resizedHeader.containedComponent.getX(), 20);

                        songListWidget.resizedHeader = null;
                        songListWidget.resizeLine = -1;
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
                    } else if(songListWidget.dragging) {
                        songListWidget.dragging = false;
                        songListWidget.draggedHeader = null;
                        songListWidget.dragStart = -1;
                        songListWidget.repaint();
                        // Everything happens in mouseDragged
                    } else {
                        songListWidget.draggedHeader = null;
                        songListWidget.dragStart = -1;

                        if(songListWidget.audio.currentSession.getSortingHeader() != TrackDataHeader.this) {
                            songListWidget.audio.currentSession.setSortingHeader(null, TrackDataHeader.this);
                            songListWidget.audio.currentSession.setSortingOrder(null, Order.DESCENDING);
                        } else {
                            // a lil confusing to read but it just flips it
                            songListWidget.audio.currentSession.setSortingOrder(null, songListWidget.audio.currentSession.getSortingOrder() == Order.DESCENDING ? Order.ASCENDING : Order.DESCENDING);
                        }
                        lastPressedX = -1;
                        // repainting the headers alone clears the lines between them, so you have to redraw the whole thing
                        // (you could define the specific regions to redraw but i dont feel like doing that)
                        songListWidget.headerPanel.repaint();

                        Library.INSTANCE.reloadSorting();
                    }
                } else if(e.isPopupTrigger()) {
                    popupMenu.show(containedComponent, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Using getButton() always returns 0 because during a drag none of the mouse buttons change state.
                if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                    if(songListWidget.resizedHeader != null) {
                        int actualX = e.getX() + containedComponent.getX() - songListWidget.resizedHeader.containedComponent.getX();
                        int oldPos = songListWidget.resizeLine;
                        if(actualX < 20) {
                            // can safely assume other headers' containedComponents are non-null since the user is dragging on this one
                            songListWidget.resizeLine = songListWidget.resizedHeader.containedComponent.getX() + 20;
                        } else {
                            songListWidget.resizeLine = containedComponent.getX() + e.getX();
                        }
                        // repainting everything uses an unreasonable amount of gpu
                        songListWidget.repaint(oldPos, 0, 1, songListWidget.getHeight());
                        songListWidget.repaint(songListWidget.resizeLine, 0, 1, songListWidget.getHeight());
                    } else if(songListWidget.draggedHeader != null) {
                        if(songListWidget.dragging) {

                            // Swing doesn't have time to react without a small cooldown
                            if(Instant.now().toEpochMilli() - songListWidget.lastDragMove.toEpochMilli() < 20) return;

                            int currentIndex = songListWidget.dataHeaders.indexOf(songListWidget.draggedHeader);
                            boolean changes = false;
                            int mousePos = e.getX();

                            for(int i = 0; i < songListWidget.dataHeaders.indexOf(TrackDataHeader.this); i++) {
                                TrackDataHeader target = songListWidget.dataHeaders.get(i);
                                if(target == null) break;
                                mousePos += target.width;
                            }

                            for(int i = 0, x = 0; i < songListWidget.dataHeaders.size(); i++) {
                                TrackDataHeader target = songListWidget.dataHeaders.get(i);
                                if(target == null) break;

                                if(i < currentIndex) {
                                    if(mousePos < x + target.width / 2) {
                                        changes = true;
                                        songListWidget.dataHeaders.remove(currentIndex);
                                        songListWidget.dataHeaders.add(i, songListWidget.draggedHeader);
                                        break;
                                    }
                                } else if(i > currentIndex) {
                                    if(mousePos > x + target.width / 2) {
                                        changes = true;
                                        songListWidget.dataHeaders.remove(currentIndex);
                                        songListWidget.dataHeaders.add(i, songListWidget.draggedHeader);
                                        break;
                                    }
                                }

                                x += target.width;
                            }

                            if(changes) {
                                songListWidget.refreshHeadersNow();
                                songListWidget.refreshTracksNow();
                                songListWidget.repaint();
                                songListWidget.headerPanel.repaint();
                                songListWidget.lastDragMove = Instant.now();
                            }
                        } else {
                            if(Math.abs(e.getX() - songListWidget.dragStart) >= 20) {
                                songListWidget.dragging = true;
                                component.repaint();
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON1) {
                    if(wasPressing) return;
                    wasPressing = true;
                    lastPressedX = e.getX();
                    if(e.getX() < 20) {
                        int index = songListWidget.dataHeaders.indexOf(TrackDataHeader.this) - 1;
                        if(index >= 0) {
                            songListWidget.resizedHeader = songListWidget.dataHeaders.get(index);
                        } else {
                            songListWidget.resizedHeader = null;
                        }
                    } else if(e.getX() > width - 20) {
                        songListWidget.resizedHeader = TrackDataHeader.this;
                    } else {
                        songListWidget.draggedHeader = TrackDataHeader.this;
                        songListWidget.dragStart = e.getX();
                    }
                } else if(e.isPopupTrigger()) {
                    popupMenu.show(containedComponent, e.getX(), e.getY());
                }
            }
        };

        containedComponent.addMouseListener(listener);
        containedComponent.addMouseMotionListener(listener);

        return containedComponent;
    }

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

    public static TrackDataHeader getById(int id) {
        for(TrackDataHeader header : Main.songListWidget.dataHeaders) {
            if(header.id == id) return header;
        }
        return null;
    }

}
