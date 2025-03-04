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

package dev.blackilykat.widgets.tracklist;

import dev.blackilykat.Library;
import dev.blackilykat.Track;
import dev.blackilykat.util.Pair;

import javax.swing.JComponent;

public interface TrackDataEntry<T extends TrackDataEntry<T>> {
    /**
     * Compares this entry to another of the same type.
     * @param other the entry to compare this to.
     * @return 0 if they're equal, >0 if this is greater than {@code other}, or <0 if {@code other} is greater than this.
     */
    int compare(T other);

    /**
     * @return The gui component that will be used to display the entry's information.
     */
    JComponent getComponent();

    /**
     * @return How the gui component should be aligned.
     */
    Alignment getAlignment();

    enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    @SuppressWarnings("unchecked")
    static TrackDataEntry<? extends TrackDataEntry<?>> create(Track track, TrackDataHeader header) {
        if(header.clazz == IntegerTrackDataEntry.class) {
            return IntegerTrackDataEntry.create(track, header);
        }
        if(header.clazz == StringTrackDataEntry.class) {
            return StringTrackDataEntry.create(track, header);
        }
        if(header.clazz == TimeTrackDataEntry.class) {
            return TimeTrackDataEntry.create(track, header);
        }
        return null;
    }

    static Class<? extends TrackDataEntry<?>> getEntryType(String key, Library library) {
        if(key.equals("duration")) return TimeTrackDataEntry.class;

        boolean withKey = false, numbersOnly = true;
        mainLoop: for(Track track : library.tracks) {
            for(Pair<String, String> metadatum : track.metadata) {
                if(!metadatum.key.equals(key)) continue;
                withKey = true;
                for(int i = 0; i < metadatum.value.length(); i++) {
                    char c = metadatum.value.charAt(i);
                    if(c < '0' || c > '9') {
                        numbersOnly = false;
                        break mainLoop;
                    }
                }
            }
        }
        if(!withKey || !numbersOnly) return StringTrackDataEntry.class;
        return IntegerTrackDataEntry.class;
    }
}
