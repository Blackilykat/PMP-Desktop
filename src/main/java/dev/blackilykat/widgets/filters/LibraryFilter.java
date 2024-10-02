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

package dev.blackilykat.widgets.filters;

import dev.blackilykat.Library;
import dev.blackilykat.util.Pair;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class LibraryFilter implements Serializable {
    /**
     * Used to indicate that the "all" option is selected.
     */
    public static final String OPTION_EVERYTHING = "__PMP_OPTION_EVERYTHING__";
    /**
     * Used to indicate that a track does not have this {@link #key}.
     */
    public static final String OPTION_UNKNOWN = "__PMP_OPTION_UNKNOWN__";

    /**
     * The library this filter is filtering
     */
    public final Library library;
    /**
     * The FLAC metadata key this filter is based on. Some examples can be <code>artist</code> and <code>album</code>.
     */
    public String key;
    /**
     * A list containing all the tracks that match this filter.
     */
    public Set<Library.Track> matchingTracks = new HashSet<>();
    /**
     * All options the user can select.
     */
    private Set<LibraryFilterOption> options = new HashSet<>();

    /**
     * The panel that represents this filter.
     */
    public LibraryFilterPanel panel = null;

    public LibraryFilter(Library library, String key) {
        this.library = library;
        this.key = key;
        if(library != null) {
            library.filters.add(this);
        }
    }

    public LibraryFilterOption[] getOptions() {
        return options.toArray(new LibraryFilterOption[0]);
    }

    public LibraryFilterOption getOption(String value) {
        for(LibraryFilterOption option : this.getOptions()) {
            if(option.value.equals(value)) {
                return option;
            }
        }
        return null;
    }

    public void reloadMatching() {
        matchingTracks.clear();

        if(this.getOption(OPTION_EVERYTHING).state == LibraryFilterOption.State.POSITIVE) {
            matchingTracks.addAll(library.filteredTracks);
        } else {
            for(LibraryFilterOption option : getOptions()) {
                if(option.state != LibraryFilterOption.State.POSITIVE) continue;

                for(Library.Track track : library.filteredTracks) {
                    boolean hasKey = false;
                    System.out.println("Filtering for track " + track.title);
                    for(Pair<String, String> pair : track.metadata) {
                        if(!key.equals(pair.key)) continue;
                        System.out.println("key " + key + " value " + pair.value);

                        hasKey = true;
                        if(option.value.equals(pair.value)) {
                            matchingTracks.add(track);
                        }
                    }

                    LibraryFilterOption unknown = this.getOption(OPTION_UNKNOWN);
                    // reloadOptions() would've added unknown to the options list due to this not having the key
                    assert unknown != null;
                    if(!hasKey && unknown.state == LibraryFilterOption.State.POSITIVE) {
                        matchingTracks.add(track);
                    }
                }
            }
        }

        for(LibraryFilterOption option : getOptions()) {
            if(option.state != LibraryFilterOption.State.NEGATIVE) continue;

            for(Library.Track track : library.filteredTracks) {
                boolean hasKey = false;

                for(Pair<String, String> pair : track.metadata) {
                    if(!key.equals(pair.key)) continue;

                    hasKey = true;
                    if(option.value.equals(pair.value)) {
                        matchingTracks.remove(track);
                    }
                }

                LibraryFilterOption unknown = this.getOption(OPTION_UNKNOWN);
                assert unknown != null;
                if(!hasKey && unknown.state == LibraryFilterOption.State.NEGATIVE) {
                    matchingTracks.remove(track);
                }
            }
        }
    }

    public void reloadOptions() {
        options.clear();
        LibraryFilterOption everything = new LibraryFilterOption(this, OPTION_EVERYTHING);
        everything.state = LibraryFilterOption.State.POSITIVE;
        options.add(everything);

        for(Library.Track track : library.tracks) {
            boolean hasKey = false;

            for(Pair<String, String> pair : track.metadata) {
                if(!key.equals(pair.key)) continue;

                hasKey = true;
                options.add(new LibraryFilterOption(this, pair.value));
            }

            if(!hasKey) {
                options.add(new LibraryFilterOption(this, OPTION_UNKNOWN));
            }
        }

        if(panel != null) {
            panel.reloadOptions();
        }
    }
}
