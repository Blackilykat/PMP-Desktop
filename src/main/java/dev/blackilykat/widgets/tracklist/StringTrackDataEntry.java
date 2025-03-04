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

import dev.blackilykat.Track;
import dev.blackilykat.util.Pair;

import javax.swing.JComponent;
import javax.swing.JLabel;

public record StringTrackDataEntry(String data) implements TrackDataEntry<StringTrackDataEntry> {
    @Override
    public int compare(StringTrackDataEntry other) {
        if(this.data == null && other.data == null) {
            return 0;
        } else if(this.data == null) {
            return -1;
        } else if(other.data == null) {
            return 1;
        }

        return this.data.compareTo(other.data);
    }

    @Override
    public JComponent getComponent() {
        return new JLabel(data);
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    public static StringTrackDataEntry create(Track track, TrackDataHeader header) {
        StringBuilder builder = new StringBuilder();
        for(Pair<String, String> metadatum : track.metadata) {
            if(metadatum.key.equalsIgnoreCase(header.metadataKey)) {
                if(!builder.isEmpty()) builder.append(", ");
                builder.append(metadatum.value);
            }
        }
        if(!builder.isEmpty()) return new StringTrackDataEntry(builder.toString());
        return new StringTrackDataEntry(null);
    }
}
