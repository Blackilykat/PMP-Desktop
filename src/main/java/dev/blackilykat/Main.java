package dev.blackilykat;

import dev.blackilykat.widgets.filters.LibraryFiltersWidget;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import dev.blackilykat.widgets.SongListWidget;
import dev.blackilykat.widgets.SongPathWidget;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.net.InetAddress;

public class Main {
    public static JFrame mainWindow;
    public static GridBagLayout mainWindowLayout;
    public static SongPathWidget songPathWidget;
    public static PlayBarWidget playBarWidget;
    public static SongListWidget songListWidget;
    public static LibraryFiltersWidget libraryFiltersWidget;

    public static void main(String[] args) {
        // enable text antialiasing cause its off by default for some stupid reason
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        SwingUtilities.invokeLater(() -> {
            mainWindow = new JFrame("PMP Desktop");
            mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainWindow.setSize(700, 400);

            mainWindowLayout = new GridBagLayout();
            mainWindow.setLayout(mainWindowLayout);

            GridBagConstraints constraints = new GridBagConstraints();

            SongPathWidget tempPanel = new SongPathWidget();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTH;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            mainWindow.add(tempPanel, constraints);

            constraints = new GridBagConstraints();

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


            Main.songPathWidget = tempPanel;
            Main.playBarWidget = playBarWidget;
            Main.songListWidget = songListWidget;
            Main.libraryFiltersWidget = libraryFiltersWidget;
            Library.INSTANCE.reload();

            mainWindow.setVisible(true);
        });
        Storage.init();
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
            ServerConnection.INSTANCE = new ServerConnection(InetAddress.getByName("localhost"), 5000);
            ServerConnection.INSTANCE.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}