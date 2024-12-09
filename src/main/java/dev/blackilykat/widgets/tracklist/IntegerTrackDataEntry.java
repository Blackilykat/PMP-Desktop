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

public record IntegerTrackDataEntry(Integer data) implements TrackDataEntry<IntegerTrackDataEntry> {

    @Override
    public int compare(IntegerTrackDataEntry other) {
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
        return new JLabel(data.toString());
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    public static IntegerTrackDataEntry create(Track track, TrackDataHeader<IntegerTrackDataEntry> header) {
        for(Pair<String, String> metadatum : track.metadata) {
            if(metadatum.key.equals(header.metadataKey)) {
                return new IntegerTrackDataEntry(Integer.valueOf(metadatum.value));
            }
        }
        return new IntegerTrackDataEntry(null);
    }
}
