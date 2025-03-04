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

import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.tracklist.TrackPanel;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Track {
    public String title;
    private File file;
    public TrackPanel panel = null;
    public List<Pair<String, String>> metadata = new ArrayList<>();
    /**
     * CRC32 checksum of the track
     */
    public long checksum = -1;
    /**
     * The pcm audio data for this track. Should only be anything other than null when it's either being played or
     * about to.
     */
    public byte[] pcmData = null;
    /**
     * How many bytes of {@link #pcmData} are loaded.
     */
    public int loaded = 0;
    public int durationSeconds = 0;

    public Track(File path) {
        this.file = path;
        if(path.getName().endsWith(".flac")) {
            try {
                FLACDecoder decoder = new FLACDecoder(new FileInputStream(file));
                Metadata[] metadataList = decoder.readMetadata();
                for(Metadata metadataItem : metadataList) {
                    if(metadataItem instanceof StreamInfo streamInfo) {
                        durationSeconds = (int) (streamInfo.getTotalSamples() / streamInfo.getSampleRate());
                        continue;
                    }
                    if(!(metadataItem instanceof VorbisComment commentMetadata)) {
                        continue;
                    }
                    String title = "";
                    ArrayList<String> artists = new ArrayList<>();
                    for(VorbisString comment : commentMetadata.getComments()) {
                        String[] parts = comment.toString().split("=");
                        // metadata can have = symbol, the key can't. only the first = splits key and value.
                        Pair<String, String> pair = new Pair<>(parts[0], Arrays.stream(parts).skip(1).collect(Collectors.joining("=")));
                        metadata.add(pair);
                        if(pair.key.equalsIgnoreCase("title")) {
                            title = pair.value;
                        } else if(pair.key.equalsIgnoreCase("artist")) {
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
            } catch(IOException ignored) {
            }
        } else {
            this.title = path.getName();
        }
    }

    public File getFile() {
        return this.file;
    }
}
