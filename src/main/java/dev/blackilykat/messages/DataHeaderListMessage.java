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

package dev.blackilykat.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blackilykat.Library;
import dev.blackilykat.Main;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.messages.exceptions.MessageInvalidContentsException;
import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.tracklist.TrackDataEntry;
import dev.blackilykat.widgets.tracklist.TrackDataHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataHeaderListMessage extends Message {
    public static final String MESSAGE_TYPE = "DATA_HEADER_LIST";

    public List<Pair<String, String>> headers = new ArrayList<>();

    public DataHeaderListMessage() {
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        JsonArray arr = new JsonArray();
        for(Pair<String, String> header : headers) {
            // could make it a key/value pair using JSON keys and values, but it's a mess to deal with that, and it would
            // prevent having duplicate headers which - while it would make little sense - should be perfectly valid
            JsonObject obj = new JsonObject();
            obj.addProperty("key", header.key);
            obj.addProperty("name", header.value);
            arr.add(obj);
        }
        object.add("headers", arr);
    }

    public static Message fromJson(JsonObject json) throws MessageException {
        DataHeaderListMessage msg = new DataHeaderListMessage();
        try {
            for(JsonElement headerElem : json.getAsJsonArray("headers")) {
                JsonObject header = (JsonObject) headerElem;
                msg.headers.add(new Pair<>(header.get("key").getAsString(), header.get("name").getAsString()));
            }
        } catch(UnsupportedOperationException | ClassCastException e) {
            throw new MessageInvalidContentsException("Expected list of strings, found something else");
        }
        return msg;
    }

    @Override
    public void handle(ServerConnection connection) {
        TrackDataHeader[] oldHeaders = Main.songListWidget.dataHeaders.toArray(new TrackDataHeader[0]);
        Main.songListWidget.dataHeaders.clear();

        newHeaderLoop: for(Pair<String, String> header : headers) {
            for(TrackDataHeader oldHeader : oldHeaders) {
                if(oldHeader.metadataKey.equals(header.key)) {
                    Main.songListWidget.dataHeaders.add(oldHeader);
                    oldHeader.name = header.value;
                    continue newHeaderLoop;
                }
            }

            // arbitrarily chose 250 pixels as a default value for the width
            Main.songListWidget.dataHeaders.add(new TrackDataHeader(header.value, header.key, TrackDataEntry.getEntryType(header.key, Library.INSTANCE), 250, Main.songListWidget));
        }

        Main.songListWidget.refreshHeaders();
        Main.songListWidget.refreshTracks();
        Main.songListWidget.repaint();
    }
}
