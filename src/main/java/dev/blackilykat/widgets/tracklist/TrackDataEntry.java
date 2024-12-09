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

package dev.blackilykat.widgets.tracklist;

import dev.blackilykat.Track;

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
    public static TrackDataEntry<? extends TrackDataEntry<?>> create(Track track, TrackDataHeader<? extends TrackDataEntry<?>> header) {
        if(header.clazz == IntegerTrackDataEntry.class) {
            return IntegerTrackDataEntry.create(track, (TrackDataHeader<IntegerTrackDataEntry>) header);
        }
        if(header.clazz == StringTrackDataEntry.class) {
            return StringTrackDataEntry.create(track, (TrackDataHeader<StringTrackDataEntry>) header);
        }
        if(header.clazz == TimeTrackDataEntry.class) {
            return TimeTrackDataEntry.create(track, (TrackDataHeader<TimeTrackDataEntry>) header);
        }
        return null;
    }
}
