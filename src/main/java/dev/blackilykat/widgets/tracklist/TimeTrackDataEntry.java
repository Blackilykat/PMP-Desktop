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
import dev.blackilykat.util.Pair;

import javax.swing.JComponent;
import javax.swing.JLabel;

public record TimeTrackDataEntry(Integer data) implements TrackDataEntry<TimeTrackDataEntry> {

    @Override
    public int compare(TimeTrackDataEntry other) {
        if(this.data == null && other.data == null) {
            return 0;
        } else if(this.data == null) {
            return 1;
        } else if(other.data == null) {
            return -1;
        }

        return this.data - other.data;
    }

    @Override
    public JComponent getComponent() {
        if(data == null) {
            return null;
        }
        int seconds = data % 60;
        int minutes = data / 60;
        return new JLabel(String.format("%02d:%02d", minutes, seconds));
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }


    public static TimeTrackDataEntry create(Track track, TrackDataHeader<TimeTrackDataEntry> header) {
        if(header.metadataKey.equals("duration")) {
            return new TimeTrackDataEntry(track.durationSeconds);
        }
        return new TimeTrackDataEntry(null);
    }
}
