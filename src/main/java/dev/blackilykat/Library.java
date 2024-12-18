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

import dev.blackilykat.widgets.filters.LibraryFilter;
import dev.blackilykat.widgets.filters.LibraryFilterPanel;
import dev.blackilykat.widgets.tracklist.TrackPanel;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    public Track getItem(int index) {
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
                Track track = new Track(result);
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
        for(Track element : Library.INSTANCE.filteredTracks) {
            Main.songListWidget.scrollPaneContents.add(new TrackPanel(element, Main.songListWidget));
        }
        Main.songListWidget.revalidate();
        Main.songListWidget.repaint();
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
