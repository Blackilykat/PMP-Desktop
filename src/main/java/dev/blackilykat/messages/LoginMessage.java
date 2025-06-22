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
import dev.blackilykat.messages.exceptions.MessageMissingContentsException;

public class LoginMessage extends Message {
    public static final String MESSAGE_TYPE = "LOGIN";

    // This message either contains
    public String password = null;
    public String hostname = null;
    // or
    public String token = null;
    public int deviceId = -1;

    public LoginMessage(String password, String hostname) {
        this.password = password;
        this.hostname = hostname;
    }

    public LoginMessage(String token, int deviceId) {
        this.token = token;
        this.deviceId = deviceId;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        if(password != null) {
            object.addProperty("password", password);
            object.addProperty("hostname", hostname);
        } else {
            object.addProperty("token", token);
            object.addProperty("deviceId", deviceId);
        }
    }

    @Override
    public void handle(ServerConnection connection) {
    }

    //@Override
    public static Message fromJson(JsonObject json) throws MessageException {
        if(json.has("password")) {
            return fromJsonWithPassword(json);
        } else if(json.has("token")) {
            return fromJsonWithToken(json);
        }
        throw new MessageMissingContentsException("Missing both password and token");
    };

    private static LoginMessage fromJsonWithPassword(JsonObject json) throws MessageException {
        if(!json.has("password") || !json.has("hostname")) {
            throw new MessageMissingContentsException("Missing password or hostname");
        }
        return new LoginMessage(json.get("password").getAsString(), json.get("hostname").getAsString());
    }
    private static LoginMessage fromJsonWithToken(JsonObject json) throws MessageException {
        if(!json.has("token") || !json.has("deviceId")) {
            throw new MessageMissingContentsException("Missing token or id");
        }
        return new LoginMessage(json.get("token").getAsString(), json.get("deviceId").getAsInt());
    }
}
