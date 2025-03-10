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

package dev.blackilykat.widgets.filters;

import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.Track;
import dev.blackilykat.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryFilter {
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
    public Set<Track> matchingTracks = new HashSet<>();
    /**
     * All options the user can select.
     */
    private List<LibraryFilterOption> options = new ArrayList<>();

    /**
     * The panel that represents this filter.
     */
    public LibraryFilterPanel panel = null;

    public PlaybackSession session;

    public LibraryFilter(Library library, PlaybackSession session, String key) {
        this.library = library;
        this.session = session;
        this.key = key;
    }

    public LibraryFilterOption[] getOptions() {
        return options.toArray(new LibraryFilterOption[0]);
    }

    public void setOptions(Collection<LibraryFilterOption> options) {
        this.options.clear();
        this.options.addAll(options);
        this.reloadMatching();
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

        if(this.getOption(OPTION_EVERYTHING).getState() == LibraryFilterOption.State.POSITIVE) {
            matchingTracks.addAll(library.filteredTracks);
        } else {
            for(LibraryFilterOption option : getOptions()) {
                if(option.getState() != LibraryFilterOption.State.POSITIVE) continue;

                for(Track track : library.filteredTracks) {
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
                    if(!hasKey && unknown.getState() == LibraryFilterOption.State.POSITIVE) {
                        matchingTracks.add(track);
                    }
                }
            }
        }

        for(LibraryFilterOption option : getOptions()) {
            if(option.getState() != LibraryFilterOption.State.NEGATIVE) continue;

            for(Track track : library.filteredTracks) {
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
                if(!hasKey && unknown.getState() == LibraryFilterOption.State.NEGATIVE) {
                    matchingTracks.remove(track);
                }
            }
        }
    }

    public void reloadOptions() {
        options.clear();
        LibraryFilterOption everything = new LibraryFilterOption(this, OPTION_EVERYTHING);
        everything.setState(LibraryFilterOption.State.POSITIVE, false);
        options.add(everything);
        boolean shouldAddUnknown = false;

        List<LibraryFilterOption> sortedOptions = new ArrayList<>();
        for(Track track : library.tracks) {
            boolean hasKey = false;

            for(Pair<String, String> pair : track.metadata) {
                if(!key.equals(pair.key)) continue;

                hasKey = true;
                LibraryFilterOption option = new LibraryFilterOption(this, pair.value);
                if(!sortedOptions.contains(option)) {
                    sortedOptions.add(option);
                }
            }

            if(!hasKey) {
                shouldAddUnknown = true;
            }
        }

        sortedOptions = sortedOptions.stream().sorted().toList();
        options.addAll(sortedOptions);

        if(shouldAddUnknown) {
            options.add(new LibraryFilterOption(this, OPTION_UNKNOWN));
        }

        if(panel != null) {
            panel.reloadOptions();
        }
    }
}
