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
    // or password and deviceId to reset the token

    public LoginMessage(String password, String hostname) {
        this.password = password;
        this.hostname = hostname;
    }

    public LoginMessage(String tokenOrPassword, int deviceId, boolean isToken) {
        if(isToken) {
            this.token = tokenOrPassword;
        } else {
            this.password = tokenOrPassword;
        }
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
        } else {
            object.addProperty("token", token);
        }

        if(hostname != null) {
            object.addProperty("hostname", hostname);
        }

        if(deviceId != -1) {
            object.addProperty("deviceId", deviceId);
        }
    }

    @Override
    public void handle(ServerConnection connection) {
    }

    //@Override
    public static Message fromJson(JsonObject json) throws MessageException {
        if(json.has("password")) {
            if(json.has("hostname")) {
                return new LoginMessage(json.get("password").getAsString(), json.get("hostname").getAsString());
            } else if(json.has("deviceId")) {
                return new LoginMessage(json.get("password").getAsString(), json.get("deviceId").getAsInt(), false);
            }
        } else if(json.has("token")) {
            if(!json.has("deviceId")) {
                throw new MessageMissingContentsException("Missing device id");
            }
            return new LoginMessage(json.get("token").getAsString(), json.get("deviceId").getAsInt(), true);
        }
        throw new MessageMissingContentsException("Missing both password and token");
    };
}
