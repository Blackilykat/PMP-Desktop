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

package dev.blackilykat;

import dev.blackilykat.menubar.connection.SetServerIpMenuItem;
import dev.blackilykat.widgets.filters.LibraryFiltersWidget;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import dev.blackilykat.widgets.tracklist.SongListWidget;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

public class Main {
    public static JFrame mainWindow;
    public static GridBagLayout mainWindowLayout;
    public static PlayBarWidget playBarWidget;
    public static SongListWidget songListWidget;
    public static LibraryFiltersWidget libraryFiltersWidget;

    public static void main(String[] args) {
        // enable text antialiasing cause its off by default for some stupid reason
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        Storage.init();
        Library.INSTANCE = new Library();
        Audio.INSTANCE = new Audio();

        SwingUtilities.invokeLater(() -> {
            mainWindow = new JFrame("PMP Desktop");
            mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainWindow.setSize(700, 400);

            mainWindowLayout = new GridBagLayout();
            mainWindow.setLayout(mainWindowLayout);

            GridBagConstraints constraints = new GridBagConstraints();

            LibraryFiltersWidget libraryFiltersWidget = new LibraryFiltersWidget();
            libraryFiltersWidget.setBackground(new Color(255, 0, 0));
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 0;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.VERTICAL;
            mainWindow.add(libraryFiltersWidget, constraints);

            constraints = new GridBagConstraints();

            SongListWidget songListWidget = new SongListWidget(Audio.INSTANCE);
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 2;
            constraints.weighty = 2;
            constraints.anchor = GridBagConstraints.NORTH;
            constraints.fill = GridBagConstraints.BOTH;
            mainWindow.add(songListWidget, constraints);

            constraints = new GridBagConstraints();

            PlayBarWidget playBarWidget = new PlayBarWidget();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weighty = 0;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.SOUTH;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            mainWindow.add(playBarWidget, constraints);


            Main.playBarWidget = playBarWidget;
            Main.songListWidget = songListWidget;
            Main.libraryFiltersWidget = libraryFiltersWidget;
            Library.INSTANCE.reload();

            JMenuBar menuBar = new JMenuBar();
            JMenu generalMenu = new JMenu("General");
            menuBar.add(generalMenu);

            JMenu connectionMenu = new JMenu("Connection");
            connectionMenu.add(new SetServerIpMenuItem());
            menuBar.add(connectionMenu);
            mainWindow.setJMenuBar(menuBar);

            mainWindow.setVisible(true);
        });

        synchronized(Library.INSTANCE) {
            try {
                while(!Library.INSTANCE.loaded) {
                    Library.INSTANCE.wait();
                }
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            ServerConnection.INSTANCE = new ServerConnection(Storage.getServerIp(), Storage.getServerMainPort(), Storage.getServerFilePort());
            ServerConnection.INSTANCE.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}