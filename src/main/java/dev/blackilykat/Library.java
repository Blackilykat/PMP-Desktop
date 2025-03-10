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

import dev.blackilykat.widgets.filters.LibraryFilter;
import dev.blackilykat.widgets.filters.LibraryFilterOption;
import dev.blackilykat.widgets.filters.LibraryFilterPanel;
import dev.blackilykat.widgets.tracklist.Order;
import dev.blackilykat.widgets.tracklist.TrackDataEntry;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Library {
    public static Library INSTANCE = null;
    public Audio audio = null;
    public List<Track> tracks = new ArrayList<>();
    public List<Track> filteredTracks = new ArrayList<>();
    public boolean loaded = false;

    public Library() {
        this.reloadAll();
    }

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

    public synchronized void reloadTracks() {
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
    }

    public synchronized void reloadAll() {
        reloadTracks();
        reloadOptions();
        reloadFilters();
        reloadSorting();
    }

    public void reloadOptions() {
        if(Main.libraryFiltersWidget != null) {
            for(LibraryFilter filter : audio.currentSession.getLibraryFilters()) {
                LibraryFilterOption[] oldOptions = filter.getOptions();
                filter.reloadOptions();
                for(LibraryFilterOption oldOption : oldOptions) {
                    LibraryFilterOption newOption = filter.getOption(oldOption.value);
                    if(newOption == null) continue;
                    newOption.state = oldOption.state;
                }
            }
        }
    }

    public void reloadFilters() {
        filteredTracks.clear();
        filteredTracks.addAll(tracks);
        if(audio == null) return;
        System.out.println("RELOADING FILTERS");
        for(LibraryFilter filter : audio.currentSession.getLibraryFilters()) {
            System.out.println("FILTER " + filter.key);
            filter.reloadMatching();
            filteredTracks.clear();
            filteredTracks.addAll(filter.matchingTracks);
        }

    }

    public void reloadSorting() {
        if(Main.songListWidget == null || Main.songListWidget.orderingHeader == null) return;

        //TODO make reloadSorting not depend on the GUI elements so it doesn't have to reload this twice
        Main.songListWidget.refreshTracks();

        int dataIndex = Main.songListWidget.dataHeaders.indexOf(Main.songListWidget.orderingHeader);
        final int multiplier = Main.songListWidget.order == Order.DESCENDING ? 1 : -1;

        int i = 0;
        for(Track track : filteredTracks.stream().sorted((o1, o2) -> {
            // jank... But if I add <?> it doesn't compile cause it doesn't know if they're the same. And obviously
            // I don't know the type at compile time. I do however know that they are the same type for sure, so just
            // avoiding generics here entirely fixes the problem (even though it creates 1 billion warnings i can't get
            // rid of)
            TrackDataEntry e1 = o1.panel.dataEntries.get(dataIndex);
            TrackDataEntry e2 = o2.panel.dataEntries.get(dataIndex);
            return e1.compare(e2) * multiplier;
        }).toList()) {
            // i need it as an arraylist so i just copy over the results
            System.out.println("Ordering " + track.title);
            filteredTracks.set(i++, track);
        }

        Main.songListWidget.refreshTracks();
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
        title = withReplacedChars(title);

        StringBuilder finalString = new StringBuilder(title);
        if(album != null) {
            album = withReplacedChars(album);
            finalString.append("_").append(album);
        }
        if(!artists.isEmpty()) {
            artists = withReplacedChars(artists);
            finalString.append("_").append(artists);
        }
        finalString.append(".flac");
        return finalString.toString();
    }

    private static String withReplacedChars(String s) {
        char[] chars = s.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            if(Character.UnicodeScript.of(chars[i]) != Character.UnicodeScript.COMMON) continue;
            if(chars[i] >= '0' && chars[i] <= '9') continue;
            chars[i] = '_';
        }
        return new String(chars);
    }
}
