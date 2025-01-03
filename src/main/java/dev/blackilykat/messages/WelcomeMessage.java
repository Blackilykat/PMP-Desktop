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

package dev.blackilykat.messages;

import com.google.gson.JsonObject;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.messages.exceptions.MessageException;

/**
 * The first message the server sends when a client successfully connects (would be after authentication when that gets
 * implemented), used to confirm that it successfully connected and to communicate its
 * {@link dev.blackilykat.ServerConnection#clientId}
 */
public class WelcomeMessage extends Message {
    public static final String MESSAGE_TYPE = "WELCOME";
    /**
     * If the client is waiting for library actions it has missed through a {@link LibraryActionRequestMessage}, the
     * {@link LibraryActionMessage#actionId} of the last missed action it should receive. Else -1. <br />
     * Used to make sure the {@link LibraryHashesMessage} doesn't get handled before those actions are received causing
     * desync (for example, other client removes a track from library but this client still has it and hasn't received
     * the remove action yet so it sees the server is missing that track and tries to add it back with an outdated
     * {@link dev.blackilykat.messages.LibraryActionMessage#actionId})
     */
    public static int waitingForMissedActions = -1;
    /**
     * @see dev.blackilykat.ServerConnection#clientId
     */
    public int clientId;
    /**
     * The latest library action id so the client can catch up if needed before checking checksums
     */
    public int latestActionId;

    public WelcomeMessage(int clientId, int latestActionId) {
        if(clientId < 0) {
            throw new IllegalArgumentException("Client ID must be greater or equal than 0");
        }
        if(latestActionId < 0) {
            throw new IllegalArgumentException("Latest action ID must be greater or equal than 0");
        }
        this.clientId = clientId;
        this.latestActionId = latestActionId;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.addProperty("client_id", clientId);
        object.addProperty("latest_action_id", latestActionId);
    }

    @Override
    public void handle(ServerConnection connection) {
        connection.connected = true;
        connection.clientId = clientId;
        int currentActionId = Storage.getCurrentActionID();
        if(currentActionId != -1 && currentActionId < latestActionId) {
            waitingForMissedActions = latestActionId - 1;
            LibraryActionRequestMessage message = new LibraryActionRequestMessage(currentActionId);
            connection.send(message);
        // not just else because if its not -1 and is the same as the latest it should do nothing
        } else if(currentActionId == -1 || currentActionId > latestActionId) {
            Storage.setCurrentActionID(latestActionId);
        }
        System.out.println("Successfully connected to server with client ID " + clientId);
        ServerConnection.callConnectListeners(connection);
    }

    //@Override
    public static Message fromJson(JsonObject json) throws MessageException {
        return new WelcomeMessage(json.get("client_id").getAsInt(), json.get("latest_action_id").getAsInt());
    }
}
