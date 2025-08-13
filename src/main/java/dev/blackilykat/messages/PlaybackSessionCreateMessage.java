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
import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.exceptions.MessageException;

/**
 * Message to create a playback session.
 * When the client sends this, it may only set a requestId to ask the server to create a session. The requestId must be
 * a sessionId that's unique to the client.
 * The server will then respond to that client with the requestId sent by the client and the responseId being the actual
 * id of the playback session.
 * Other clients will receive a message only containing the response id, since they did not request the creation of that
 * session.
 */
public class PlaybackSessionCreateMessage extends Message {
    public static final String MESSAGE_TYPE = "PLAYBACK_SESSION_CREATE";
    Integer requestId;
    Integer responseId;

    public PlaybackSessionCreateMessage(Integer requestId, Integer responseId) {
        this.requestId = requestId;
        this.responseId = responseId;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        if(requestId != null) {
            object.addProperty("requestId", requestId);
        }
        if(responseId != null) {
            object.addProperty("responseId", responseId);
        }
    }

    @Override
    public void handle(ServerConnection connection) {
        boolean shouldCreate = true;
        if(requestId != null) {
            for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
                if(session.id != requestId) continue;
                session.id = responseId;
                session.acknowledgedByServer = true;
                session.callUpdateListeners();
                if(session.getOwnerId() == connection.clientId) {
                    connection.send(new PlaybackSessionUpdateMessage(responseId, null, null, null, null, null, connection.clientId, null, null, null, null));
                }
                shouldCreate = false;
                break;
            }
        }
        if(shouldCreate) {
            PlaybackSession session = new PlaybackSession(Audio.INSTANCE, responseId);
            session.acknowledgedByServer = true;
            session.register();
        }

        PlaybackSession.idCounter = requestId;
    }

    //@Override
    public static PlaybackSessionCreateMessage fromJson(JsonObject json) throws MessageException {
        return new PlaybackSessionCreateMessage(
                json.has("requestId") ? json.get("requestId").getAsInt() : null,
                json.has("responseId") ? json.get("responseId").getAsInt() : null
        );
    }
}
