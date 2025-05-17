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
import dev.blackilykat.Main;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.Track;
import dev.blackilykat.messages.DataHeaderListMessage;
import dev.blackilykat.util.Pair;
import dev.blackilykat.util.Triple;
import dev.blackilykat.widgets.ScrollablePanel;
import dev.blackilykat.widgets.Widget;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongListWidget extends Widget {
    public final Audio audio;
    public JPanel scrollPaneContents = new ScrollablePanel();
    public JScrollPane scrollPane = new JScrollPane(scrollPaneContents);
    public TrackDataHeaderContainer headerPanel = new TrackDataHeaderContainer(this);
    public static JPopupMenu trackListPopupMenu = new JPopupMenu();
    public JPopupMenu headerPopupMenu = new JPopupMenu();
    public List<TrackDataHeader> dataHeaders = new ArrayList<>();
    // -1: don't show, other: x coordinate of the line that will display while resizing
    public int dragResizeLine = -1;
    public TrackDataHeader draggedHeader = null;

    static {
        trackListPopupMenu.add(getAddTrackPopupItem());
    }

    public SongListWidget(Audio audio) {
        super();
        this.audio = audio;

        List<Triple<String, String, Integer>> storedHeaders = Storage.getTrackHeaders();
        if(storedHeaders != null) {
            for(Triple<String, String, Integer> header : storedHeaders) {
                dataHeaders.add(new TrackDataHeader(header.a, header.b, TrackDataEntry.getEntryType(header.b, Library.INSTANCE), header.c, this));
            }
        } else {
            dataHeaders.add(new TrackDataHeader("N°", "tracknumber", IntegerTrackDataEntry.class, 50, this));
            dataHeaders.add(new TrackDataHeader("Title", "title", StringTrackDataEntry.class, 500, this));
            dataHeaders.add(new TrackDataHeader("Artist", "artist", StringTrackDataEntry.class, 300, this));
            dataHeaders.add(new TrackDataHeader("Length", "duration", TimeTrackDataEntry.class, 100, this));
        }

        this.add(headerPanel);
        layout.putConstraint(SpringLayout.NORTH, headerPanel, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, headerPanel, 32, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.EAST, headerPanel, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.WEST, headerPanel, 0, SpringLayout.WEST, this);

        scrollPaneContents.setLayout(new BoxLayout(scrollPaneContents, BoxLayout.Y_AXIS));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.add(scrollPane);
        layout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.SOUTH, headerPanel);
        layout.putConstraint(SpringLayout.WEST, scrollPane, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, scrollPane, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, scrollPane, 0, SpringLayout.EAST, this);

        scrollPaneContents.addMouseListener(new MouseAdapter() {
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
                    trackListPopupMenu.show(SongListWidget.this, e.getX(), e.getY());
                }
            }
        });

        headerPanel.addMouseListener(new MouseAdapter() {
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
                    headerPopupMenu.show(SongListWidget.this, e.getX(), e.getY());
                }
            }
        });
        refreshHeaders();

        headerPopupMenu.add(getAddHeaderPopupItem());
    }

    public void refreshHeaders() {
        headerPanel.removeAll();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        for(TrackDataHeader dataHeader : dataHeaders) {
            headerPanel.add(dataHeader.getContainedComponent());
        }
    }

    public void refreshTracks() {
        scrollPaneContents.removeAll();
        for(Track element : Library.INSTANCE.filteredTracks) {
            scrollPaneContents.add(new TrackPanel(element, this));
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.getParent().getWidth(), -1);
    }

    // insanity but it only appears in 1 menu if i use the same object
    public static JMenuItem getAddTrackPopupItem() {
        JMenuItem item = new JMenuItem("Add track");
        item.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                run();
            }

            private void run() {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setMultiSelectionEnabled(true);
                    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    int returnValue = chooser.showOpenDialog(null);
                    if(returnValue != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    List<File> files = recurseInDirectories(chooser.getSelectedFiles());
                    for(File originalFile : files) {
                        if(!originalFile.getName().endsWith(".flac")) {
                            System.out.println("Skipping " + originalFile.getName() + " (no .flac extension)");
                            continue;
                        }

                        try(InputStream is = new FileInputStream(originalFile)) {
                            byte[] expectedBtyes = new byte[]{'f', 'L', 'a', 'C'};
                            byte[] actualBytes = new byte[4];
                            if(is.read(actualBytes) < 4 || !Arrays.equals(expectedBtyes, actualBytes)) {
                                System.out.println("Skipping " + originalFile.getName() + " (no fLaC magic)");
                                continue;
                            }
                        }

                        File newFile = new File(Storage.LIBRARY, Library.getNewFileName(originalFile));
                        try {
                            Files.copy(originalFile.toPath(), newFile.toPath());
                        } catch(FileAlreadyExistsException e) {
                            //TODO prompt user for confirmation, for now it's a good enough guess to not add it since it
                            //     takes the filename, artist and album to build the filename
                            continue;
                        }

                        if(ServerConnection.INSTANCE != null) {
                            ServerConnection.INSTANCE.sendAddTrack(newFile.getName());
                        } else {
                            Storage.pushPendingLibraryAction(new LibraryAction(newFile.getName(), LibraryAction.Type.ADD));
                        }
                    }
                    Library.INSTANCE.reloadAll();

                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return item;
    }

    private static List<File> recurseInDirectories(File... files) {
        if(files == null) return List.of();
        List<File> result = new ArrayList<>();
        for(File file : files) {
            if(file == null) continue;
            if(!file.exists()) continue;
            if(!file.isDirectory()) {
                result.add(file);
                continue;
            }
            result.addAll(recurseInDirectories(file.listFiles()));
        }
        return result;
    }

    public JMenuItem getAddHeaderPopupItem() {
        JMenuItem item = new JMenuItem("Add header");
        item.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                run();
            }

            private void run() {
                try {
                    // ask for label (string) and key (string)
                    JTextField labelField = new JTextField(15);
                    JTextField keyField = new JTextField(15);
                    int r = JOptionPane.showConfirmDialog(null,
                            new Object[]{"Label: ", labelField, "Key: ", keyField},
                            "New header",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);

                    if(r != JOptionPane.YES_OPTION) {
                        return;
                    }

                    dataHeaders.add(new TrackDataHeader(labelField.getText(), keyField.getText(), TrackDataEntry.getEntryType(keyField.getText(), Library.INSTANCE), 50, SongListWidget.this));
                    SongListWidget.this.refreshHeaders();
                    Main.songListWidget.refreshTracks();

                    if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                        DataHeaderListMessage msg = new DataHeaderListMessage();
                        for(TrackDataHeader header : SongListWidget.this.dataHeaders) {
                            msg.headers.add(new Pair<>(header.metadataKey, header.name));
                        }
                        ServerConnection.INSTANCE.send(msg);
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        return item;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if(dragResizeLine > -1) {
            g.setColor(Color.BLACK);
            g.fillRect(dragResizeLine, 0, 1, this.getHeight() - 2);
        }
    }
}
