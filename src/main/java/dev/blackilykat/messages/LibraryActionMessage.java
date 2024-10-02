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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blackilykat.Json;
import dev.blackilykat.Library;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to notify of changes in the library. For ADD and REPLACE, the server should wait about 10 seconds for a
 * connection to be made to the http server so the clients can upload their files. Clients can expect a successful
 * transfer if the http server sends back a 200. When clients get an ADD or REPLACE message from the server they can
 * rely on the http server to get the file as well.
 */
public class LibraryActionMessage extends Message {
    public static final String MESSAGE_TYPE = "LIBRARY_ACTION";
    public int actionId;
    public Type actionType;
    public String fileName;
    public List<Pair<String, String>> newMetadata;

    public LibraryActionMessage(Type type, int actionId, String fileName) {
        if(type == Type.CHANGE_METADATA) {
            throw new IllegalArgumentException("Incorrect initializer: expected List<Pair<String, String>> as fourth argument for action type " + type);
        }
        this.actionType = type;
        this.actionId = actionId;
        this.fileName = fileName;
    }

    public LibraryActionMessage(Type type, int actionId, String fileName, List<Pair<String, String>> newMetadata) {
        if(type != Type.CHANGE_METADATA) {
            throw new IllegalArgumentException("Incorrect initializer: expected only three arguments for action type " + type);
        }
        this.actionType = type;
        this.actionId = actionId;
        this.fileName = fileName;
        this.newMetadata = newMetadata;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.addProperty("action_type", actionType.toString());
        object.addProperty("action_id", actionId);
        object.addProperty("file_name", fileName);
        if(actionType == Type.CHANGE_METADATA) {
            object.add("new_metadata", Json.GSON.toJsonTree(newMetadata));
        }
    }

    //@Override
    public static LibraryActionMessage fromJson(JsonObject json) throws MessageException {
        Type type = Type.valueOf(json.get("action_type").getAsString());
        if(type == Type.CHANGE_METADATA) {
            List<Pair<String, String>> metadata = new ArrayList<>();
            for (JsonElement metadataEntry : json.get("new_metadata").getAsJsonArray()) {
                metadata.add(new Pair<>(metadataEntry.getAsJsonObject().get("key").getAsString(),
                        metadataEntry.getAsJsonObject().get("value").getAsString()));
            }
            return new LibraryActionMessage(type, json.get("action_id").getAsInt(), json.get("file_name").getAsString(), metadata);
        } else {
            return new LibraryActionMessage(type, json.get("action_id").getAsInt(), json.get("file_name").getAsString());
        }
    }

    @Override
    public void handle(ServerConnection connection) {
        switch(actionType) {
            case ADD:
            case REPLACE:
                connection.downloadTrack(fileName);
                break;
            case REMOVE:
                new File(Storage.LIBRARY, fileName).delete();
                Library.INSTANCE.reload();
                break;
            case CHANGE_METADATA:
                // not implemented yet
                break;
        }
        Storage.setCurrentActionID(actionId+1);
        Library.INSTANCE.reload();

        if(actionId == WelcomeMessage.waitingForMissedActions) {

            if(LibraryHashesMessage.waitingToBeHandled != null) {
                LibraryHashesMessage.waitingToBeHandled.handle(connection);
                LibraryHashesMessage.waitingToBeHandled = null;
            }

            WelcomeMessage.waitingForMissedActions = -1;
        }
    }

    public static LibraryActionMessage create(Type type, String fileName) {
        int id = Storage.getCurrentActionID();
        LibraryActionMessage message = new LibraryActionMessage(type, id, fileName);
        Storage.setCurrentActionID(id+1);
        return message;
    }

    public static LibraryActionMessage create(Type type, String fileName, List<Pair<String, String>> newMetadata) {
        int id = Storage.getCurrentActionID();
        LibraryActionMessage message = new LibraryActionMessage(type, id, fileName, newMetadata);
        Storage.setCurrentActionID(id+1);
        return message;
    }

    public enum Type {
        /**
         * Add a new song to the library
         */
        ADD,
        /**
         * Remove a song from the library
         */
        REMOVE,
        /**
         * Replace the file of a song with another one (would be the same song, this action would only happen if like
         * someone changes the source, say, to get a higher quality version. This action exists so that when the
         * playback eventually gets tracked the counts don't get split or interrupted due to a file replacement)
         */
        REPLACE,
        /**
         * Change the metadata of a song while keeping the audio data untouched
         */
        CHANGE_METADATA
    }
}
