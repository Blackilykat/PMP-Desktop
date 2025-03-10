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
 * TODO remove
 * used for testing
 * not supposed to exist after testing
 * pls remove
 * ty
 */
public class TestMessage extends Message {
    public static final String MESSAGE_TYPE = "TEST";
    public int deviceId;

    public TestMessage(int deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.addProperty("device_id", deviceId);
    }

    @Override
    public void handle(ServerConnection connection) {
    }

    //@Override
    public static TestMessage fromJson(JsonObject json) throws MessageException {
        return new TestMessage(json.get("device_id").getAsInt());
    }
}
