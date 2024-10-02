package dev.blackilykat.widgets;

import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;

public class SongListWidget extends Widget {
    public final Audio audio;
    public JPanel scrollPaneContents = new ScrollablePanel();
    private JScrollPane scrollPane = new JScrollPane(scrollPaneContents);
    public static JPopupMenu popup = new JPopupMenu();

    static {
        popup.add(getAddTrackPopupItem());
    }

    public SongListWidget(Audio audio) {
        super();
        this.audio = audio;

        scrollPaneContents.setLayout(new BoxLayout(scrollPaneContents, BoxLayout.Y_AXIS));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.add(scrollPane);
        layout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.NORTH, this);
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
                    popup.show(SongListWidget.this, e.getX(), e.getY());
                }
            }
        });
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
                    System.out.println("[DEBUG] opening chooser");
                    JFileChooser chooser = new JFileChooser();
                    int returnValue = chooser.showOpenDialog(null);
                    if(returnValue != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    System.out.println("[DEBUG] chooser approved");
                    File originalFile = chooser.getSelectedFile();
                    System.out.println("[DEBUG] original file: " + originalFile.getAbsoluteFile());
                    File newFile = new File(Storage.LIBRARY, Library.getNewFileName(originalFile));
                    System.out.println("[DEBUG] new file: " + newFile.getAbsoluteFile());
                    Files.copy(originalFile.toPath(), newFile.toPath());
                    System.out.println("[DEBUG] file copied to new place");
                    Library.INSTANCE.reload();
                    System.out.println("[DEBUG] reloaded library");

                    ServerConnection.INSTANCE.sendAddTrack(newFile.getName());
                    System.out.println("[DEBUG] sent track to server, done");
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return item;
    }
}
