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

import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.LibraryAction;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.Track;
import dev.blackilykat.messages.LibraryActionMessage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static dev.blackilykat.Main.LOGGER;

public class TrackPanel extends JPanel {
    public final SongListWidget list;
    public List<TrackDataEntry<?>> dataEntries = new ArrayList<>();
    public JPopupMenu popup;
    private boolean mouseHovering = false;
    Track track;
    private Instant lastClick = null;


    public TrackPanel(Track track, SongListWidget list) {
        this.track = track;
        track.panel = this;
        this.list = list;

        BoxLayout layoutManager = new BoxLayout(this, BoxLayout.X_AXIS);
        this.setLayout(layoutManager);

        this.popup = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                run();
            }

            private void run() {
                LOGGER.info("Deleting track {}", track.getFile().getName());
                track.getFile().delete();
                if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                    ServerConnection.INSTANCE.send(LibraryActionMessage.create(LibraryAction.Type.REMOVE, track.getFile().getName()));
                } else {
                    Storage.pushPendingLibraryAction(new LibraryAction(track.getFile().getName(), LibraryAction.Type.REMOVE));
                }
                LOGGER.info("Deleted track {}", track.getFile().getName());
                Library.INSTANCE.reloadAll();
            }
        });
        this.popup.add(deleteItem);

        this.popup.add(SongListWidget.getAddTrackPopupItem());

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                maybeStartPlaying(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    TrackPanel.this.popup.show(TrackPanel.this, e.getX(), e.getY());
                }
            }

            private void maybeStartPlaying(MouseEvent e) {
                if(e.getButton() != MouseEvent.BUTTON1) return;
                Instant now = Instant.now();
                if(lastClick != null && now.toEpochMilli() - lastClick.toEpochMilli() < 500) {
                    Audio.INSTANCE.startPlaying(track, true);
                    lastClick = null;
                } else {
                    lastClick = now;
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                TrackPanel.this.mouseHovering = true;
                TrackPanel.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                TrackPanel.this.mouseHovering = false;
                TrackPanel.this.repaint();
            }
        });

        this.reloadDataEntries();
    }

    public void reloadDataEntries() {
        dataEntries.clear();
        this.removeAll();
        for(TrackDataHeader dataHeader : list.dataHeaders) {
            TrackDataEntry<?> entry = TrackDataEntry.create(track, dataHeader);
            dataEntries.add(entry);
            if(entry != null) {
                dataHeader.setAlignment(entry.getAlignment());
            }
            TrackDataEntryWidget container = new TrackDataEntryWidget(entry, dataHeader, this);
            this.add(container);
        }
        this.add(Box.createHorizontalGlue());
    }

    public boolean getMouseHovering() {
        return mouseHovering;
    }

    @Override
    public Dimension getMaximumSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(this.getParent().getWidth(), 30);
    }

    @Override
    public Color getBackground() {
        return new Color(0, 0, 0, 0);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void paintComponent(Graphics g) {
        // Race condition where the element gets removed right as it's about to be painted
        if(this.getParent() == null) return;
        g.setColor(this.getParent().getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(this.getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        if(mouseHovering) {
            g.setColor(new Color(0, 0, 0, 20));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        ui.paint(g, this);
    }
}
