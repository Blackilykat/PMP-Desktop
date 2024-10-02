package dev.blackilykat;

import dev.blackilykat.messages.LibraryActionMessage;
import dev.blackilykat.util.Icons;
import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.SongListWidget;
import dev.blackilykat.widgets.filters.LibraryFilter;
import dev.blackilykat.widgets.filters.LibraryFilterPanel;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Library {
    public static final Library INSTANCE = new Library();
    public List<Track> tracks = new ArrayList<>();
    public List<Track> filteredTracks = new ArrayList<>();
    public List<LibraryFilter> filters = new ArrayList<>();
    public boolean loaded = false;


    public int findIndex(File file) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getFile().equals(file)) return i;
        }
        return -1;
    }

    public Library.Track getItem(int index) {
        try {
            return tracks.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public synchronized void reload() {
        loaded = false;
        tracks = new ArrayList<>();
        for(File result : search(Storage.LIBRARY)) {
            if(Audio.isSupported(result)) {
                Library.Track track = new Library.Track(result, Main.songListWidget);
                tracks.add(track);

                try {
                    CheckedInputStream inputStream = new CheckedInputStream(new FileInputStream(track.getFile()), new CRC32());
                    // 1MB
                    byte[] buffer = new byte[1048576];
                    while(inputStream.read(buffer, 0, buffer.length) >= 0) {}
                    track.checksum = inputStream.getChecksum().getValue();
                } catch(IOException ignored) {
                }
            }
        }
        loaded = true;
        // allow main thread to connect to server, since library is now loaded
        synchronized(this) {
            notifyAll();
        }

        for(LibraryFilterPanel panel : Main.libraryFiltersWidget.panels) {
            panel.filter.reloadOptions();
        }

        reloadFilters();

        Main.libraryFiltersWidget.reloadElements();
    }

    private List<File> search(File path) {
        if(!path.isDirectory()) throw new IllegalArgumentException("path " + path + " isn't a directory!");
        ArrayList<File> results = new ArrayList<>();

        for(File file : path.listFiles()) {
            if(!file.isDirectory()) {
                results.add(file);
            }
        }
        return results;
    }

    public void reloadFilters() {
        filteredTracks.clear();
        filteredTracks.addAll(tracks);
        System.out.println("RELOADING FILTERS");
        for(LibraryFilter filter : filters) {
            System.out.println("FILTER " + filter.key);
            filter.reloadMatching();
            filteredTracks.clear();
            filteredTracks.addAll(filter.matchingTracks);
        }

        Main.songListWidget.scrollPaneContents.removeAll();
        for(Library.Track element : Library.INSTANCE.filteredTracks) {
            Main.songListWidget.scrollPaneContents.add(element);
        }
        Main.songListWidget.revalidate();
        Main.songListWidget.repaint();
    }

    //TODO move panel to a different class so this is more general
    public static class Track extends JPanel {
        public String title;
        private File file;
        public final SongListWidget list;
        private JButton button;
        public JLabel label;
        public JPopupMenu popup;
        public List<Pair<String, String>> metadata = new ArrayList<>();
        /**
         * CRC32 checksum of the track
         */
        public long checksum = -1;

        public Track(File path, SongListWidget list) {
            this.file = path;
            this.list = list;
            if(path.getName().endsWith(".flac")) {
                try {
                    FLACDecoder decoder = new FLACDecoder(new FileInputStream(file));
                    Metadata[] metadataList = decoder.readMetadata();
                    for(Metadata metadataItem : metadataList) {
                        if(!(metadataItem instanceof VorbisComment commentMetadata)) continue;
                        String title = "";
                        ArrayList<String> artists = new ArrayList<>();
                        for(VorbisString comment : commentMetadata.getComments()) {
                            String[] parts = comment.toString().split("=");
                            // metadata can have = symbol, the key can't. only the first = splits key and value.
                            Pair<String, String> pair = new Pair<>(parts[0], Arrays.stream(parts).skip(1).collect(Collectors.joining("=")));
                            metadata.add(pair);
                            if(pair.key.equals("title")) {
                                title = pair.value;
                            } else if (pair.key.equals("artist")) {
                                artists.add(pair.value);
                            }
                        }
                        if(title.isEmpty()) {
                            this.title = path.getName();
                        } else {
                            this.title = title
                                    + (artists.isEmpty() ? "" : " - ")
                                    + artists.stream().collect(Collectors.joining(", "));
                        }
                    }
                } catch (IOException ignored) {}
            } else {
                this.title = path.getName();
            }

            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setAlignmentX(0);

            this.button = new JButton(Icons.svgIcon(Icons.PLAY, 16, 16));
            this.button.addActionListener(new PlayButtonListener(this));
            this.add(this.button);
            this.label = new JLabel(title);
            this.add(label);

            this.popup = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("Delete");
            this.popup.add(deleteItem);
            this.popup.add(SongListWidget.getAddTrackPopupItem());
            deleteItem.addMouseListener(new MouseAdapter() {
                /*
                @Override
                public void mousePressed(MouseEvent e) {
                    run();
                }
                 */

                @Override
                public void mouseReleased(MouseEvent e) {
                    run();
                }

                private void run() {
                    System.out.println("Deleting track " + getFile().getName());
                    getFile().delete();
                    LibraryActionMessage libraryActionMessage = LibraryActionMessage.create(LibraryActionMessage.Type.REMOVE, getFile().getName());
                    ServerConnection.INSTANCE.send(libraryActionMessage);
                    System.out.println("Deleted track " + getFile().getName());
                    Library.INSTANCE.reload();
                }
            });

            this.addMouseListener(new MouseAdapter() {
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
                        Track.this.popup.show(Track.this, e.getX(), e.getY());
                    }
                }
            });
        }

        public File getFile() {
            return this.file;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(this.getParent().getWidth(), this.getMinimumSize().height);
        }

        private static class PlayButtonListener implements ActionListener {
            public final Track song;

            public PlayButtonListener(Track song) {
                this.song = song;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                song.list.audio.startPlaying(song.getFile().getPath());
            }
        }
    }

    //TODO before alpha: support foreign characters
    /**
     * Returns a new filename for a track. The new filename will be in the follwing format:<br />
     * <code>Title_Album_Artist_Artist_..._Artist.flac</code><br />
     * If any of the fields (other than the title) are missing they will be excluded from the filename, e.g.<br />
     * <code>Title_Artist.flac</code><br />
     * If the title is missing, it will return the original filename.<br />
     * If something contains spaces or special characters, they will be replaced by underscores. For example,
     * "Text? (something)" by "An artist" would become:<br />
     * <code>Text___something__An_artist.flac</code>
     */
    public static String getNewFileName(File track) {
        if(!track.isFile() || !track.getName().endsWith(".flac") || !Audio.isSupported(track)) {
            return track.getName();
        }
        String title = null;
        String album = null;
        String artists = null;

        try {
            FLACDecoder decoder = new FLACDecoder(new FileInputStream(track));
            Metadata[] metadataList = decoder.readMetadata();
            for(Metadata metadata : metadataList) {
                if(!(metadata instanceof VorbisComment commentMetadata)) continue;

                String[] titleMetadata = commentMetadata.getCommentByName("title");
                if(titleMetadata.length < 1) {
                    return track.getName();
                }
                title = titleMetadata[0];

                String[] albumMetadata = commentMetadata.getCommentByName("album");
                if(albumMetadata.length >= 1) {
                    album = albumMetadata[0];
                }

                artists = String.join("_", commentMetadata.getCommentByName("artist"));
            }
        } catch (IOException ignored) {}

        assert title != null;
        title = title.replaceAll("[^A-Za-z0-9_]", "_");

        StringBuilder finalString = new StringBuilder(title);
        if(album != null) {
            album = album.replaceAll("[^A-Za-z0-9_]", "_");
            finalString.append("_").append(album);
        }
        if(!artists.isEmpty()) {
            artists = artists.replaceAll("[^A-Za-z0-9_]", "_");
            finalString.append("_").append(artists);
        }
        finalString.append(".flac");
        return finalString.toString();
    }
}
