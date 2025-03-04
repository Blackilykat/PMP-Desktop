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

import com.google.gson.JsonObject;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.exceptions.MessageException;

/**
 * Used when a client needs to receive missing actions from the server.
 * Clients can expect the server to send all library actions from (inclusive) {@link #start} up to the latest one.
 */
public class LibraryActionRequestMessage extends Message {
    public static final String MESSAGE_TYPE = "LIBRARY_ACTION_REQUEST";
    public int start;

    public LibraryActionRequestMessage(int start) {
        this.start = start;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.addProperty("start", start);
    }

    @Override
    public void handle(ServerConnection connection) {
    }

    //@Override
    public static LibraryActionRequestMessage fromJson(JsonObject json) throws MessageException {
        return new LibraryActionRequestMessage(json.get("start").getAsInt());
    }
}
