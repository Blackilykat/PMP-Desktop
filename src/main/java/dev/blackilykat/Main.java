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

package dev.blackilykat;

import dev.blackilykat.menubar.connection.ConnectToServerMenuItem;
import dev.blackilykat.menubar.connection.DisconnectFromServerMenuItem;
import dev.blackilykat.menubar.connection.SetServerIpMenuItem;
import dev.blackilykat.menubar.debug.EnterLogMenuItem;
import dev.blackilykat.menubar.playback.ChangeSessionMenu;
import dev.blackilykat.messages.KeepAliveMessage;
import dev.blackilykat.widgets.filters.LibraryFiltersWidget;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import dev.blackilykat.widgets.tracklist.SongListWidget;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TimerTask;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static JFrame mainWindow;
    public static GridBagLayout mainWindowLayout;
    public static PlayBarWidget playBarWidget;
    public static SongListWidget songListWidget;
    public static LibraryFiltersWidget libraryFiltersWidget;

    public static void main(String[] args) {
        LOGGER.info("Starting...");

        System.setOut(loggingProxy(System.out, Level.INFO));
        System.setErr(loggingProxy(System.err, Level.ERROR));

        // enable text antialiasing cause its off by default for some stupid reason
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        Storage.init();
        Library.INSTANCE = new Library();
        Audio.INSTANCE = new Audio(Library.INSTANCE);
        Library.INSTANCE.audio = Audio.INSTANCE;

        SwingUtilities.invokeLater(() -> {
            mainWindow = new JFrame("PMP Desktop");
            mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainWindow.setSize(700, 400);

            mainWindowLayout = new GridBagLayout();
            mainWindow.setLayout(mainWindowLayout);

            GridBagConstraints constraints = new GridBagConstraints();

            LibraryFiltersWidget libraryFiltersWidget = new LibraryFiltersWidget(Audio.INSTANCE);
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
            songListWidget.refreshTracks();

            JMenuBar menuBar = new JMenuBar();
            JMenu generalMenu = new JMenu("General");
            menuBar.add(generalMenu);

            JMenu connectionMenu = new JMenu("Connection");
            connectionMenu.add(new SetServerIpMenuItem());
            connectionMenu.add(new ConnectToServerMenuItem());
            connectionMenu.add(new DisconnectFromServerMenuItem());
            menuBar.add(connectionMenu);

            JMenu playbackMenu = new JMenu("Playback");
            playbackMenu.add(new ChangeSessionMenu(Audio.INSTANCE));
            menuBar.add(playbackMenu);

            JMenu debugMenu = new JMenu("Debug");
            debugMenu.add(new EnterLogMenuItem());
            menuBar.add(debugMenu);

            mainWindow.setJMenuBar(menuBar);

            mainWindow.setVisible(true);
        });

        ServerConnection.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                    ServerConnection.INSTANCE.send(new KeepAliveMessage());
                }
            }
        }, KeepAliveMessage.KEEPALIVE_MS, KeepAliveMessage.KEEPALIVE_MS);

        new LibraryActionSenderThread().start();

        try {
            ServerConnection.INSTANCE = new ServerConnection(Storage.getServerIp(), Storage.getServerMainPort(), Storage.getServerFilePort());
        } catch (IOException e) {
            LOGGER.warn("Could not connect to server!");
        }
    }

    public static PrintStream loggingProxy(PrintStream stream, Level level) {
        String tPrefix = "(stream)";
        if(stream == System.out) tPrefix = "(stdout)";
        else if(stream == System.err) tPrefix = "(stderr)";

        final String prefix = tPrefix;

        return new PrintStream(stream) {
            @Override
            public void println() {
            }

            @Override
            public void println(int x) {
                print(x);
            }

            @Override
            public void println(char x) {
                print(x);
            }

            @Override
            public void println(long x) {
                print(x);
            }

            @Override
            public void println(float x) {
                print(x);
            }

            @Override
            public void println(char[] x) {
                print(x);
            }

            @Override
            public void println(double x) {
                print(x);
            }

            @Override
            public void println(Object x) {
                print(x);
            }

            @Override
            public void println(String x) {
                print(x);
            }

            @Override
            public void println(boolean x) {
                print(x);
            }

            @Override
            public void print(String s) {
                LOGGER.log(level, "{} {}", prefix, s);
            }

            @Override
            public void print(Object obj) {
                if(obj instanceof Exception ex) {
                    LOGGER.log(level, "{} exception", prefix, ex);
                    return;
                } else if(obj instanceof String str){
                    if(str.startsWith("\tat ")) return;
                }

                LOGGER.log(level, "{} {}", prefix, obj);
            }

            @Override
            public void print(int i) {
                LOGGER.log(level, "{} {}", prefix, i);
            }

            @Override
            public void print(char c) {
                LOGGER.log(level, "{} {}", prefix, c);
            }

            @Override
            public void print(long l) {
                LOGGER.log(level, "{} {}", prefix, l);
            }

            @Override
            public void print(boolean b) {
                LOGGER.log(level, "{} {}", prefix, b);
            }

            @Override
            public void print(float f) {
                LOGGER.log(level, "{} {}", prefix, f);
            }

            @Override
            public void print(double d) {
                LOGGER.log(level, "{} {}", prefix, d);
            }

            @Override
            public void print(char[] s) {
                LOGGER.log(level, "{} {}", prefix, s);
            }
        };
    }
}
