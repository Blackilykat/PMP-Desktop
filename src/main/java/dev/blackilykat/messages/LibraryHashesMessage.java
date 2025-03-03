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
import dev.blackilykat.LibraryAction;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.Track;
import dev.blackilykat.messages.exceptions.MessageException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends every song's filename along with its crc32 hash. Used to make sure libraries dont get desynced, which would
 * ideally happen only if someone goes out of their way to manually edit music files not through the application. This
 * would also help discover and repair desync caused due to bugs though.
 */
public class LibraryHashesMessage extends Message {
    public static final String MESSAGE_TYPE = "LIBRARY_HASHES";
    /**
     * If the client is still waiting on some missed {@link LibraryActionMessage}s (after requesting them through a
     * {@link LibraryActionRequestMessage}), the instance of the library hashes message received. If those missed
     * messages are received and the library hashes message is handled or if there were no missed messages in the first
     * place, this is null.
     * @see WelcomeMessage#waitingForMissedActions
     */
    public static LibraryHashesMessage waitingToBeHandled = null;
    public Map<String, Long> hashes;

    public LibraryHashesMessage() {
        hashes = new HashMap<>();
    }

    public LibraryHashesMessage(Map<String, Long> hashes) {
        this.hashes = hashes;
    }

    public void add(String fileName, long hash) {
        hashes.put(fileName, hash);
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.add("hashes", Json.GSON.toJsonTree(hashes));
    }

    @Override
    public void handle(ServerConnection connection) {
        if(WelcomeMessage.waitingForMissedActions > -1) {
            waitingToBeHandled = this;
            return;
        }

        // hijacking because it needs to know which hashes from the server are gonna be outdated
        // sending pending actions separately will either present race conditions or require an annoying request/response
        // system which really doesn't need to exist
        List<LibraryAction> actionsSent = new ArrayList<>();
        LibraryAction pendingAction;
        while((pendingAction = Storage.popPendingLibraryAction()) != null) {
            if(pendingAction.actionType != LibraryAction.Type.ADD) {
                LibraryActionMessage msg;
                if(pendingAction.actionType != LibraryAction.Type.CHANGE_METADATA) {
                    msg = LibraryActionMessage.create(pendingAction.actionType, pendingAction.fileName);
                } else {
                    msg = LibraryActionMessage.create(pendingAction.actionType, pendingAction.fileName, pendingAction.newMetadata);
                }
                connection.send(msg);
            } else {
                connection.sendAddTrack(pendingAction.fileName);
            }
            actionsSent.add(pendingAction);
        }

        boolean changes = false;
        for(Map.Entry<String, Long> entry : hashes.entrySet()) {
            boolean found = false;
            boolean matches = false;
            long clientValue = -1;
            for(Track track : Library.INSTANCE.tracks) {
                if(track.getFile().getName().equals(entry.getKey())) {
                    found = true;
                    if(track.checksum == entry.getValue()) {
                        matches = true;
                    }
                    clientValue = track.checksum;
                    break;
                }
            }
            if(!found) {
                System.out.println("Client doesnt have " + entry.getKey());
                boolean nevermind = false;
                for(LibraryAction libraryAction : actionsSent) {
                    if(libraryAction.actionType != LibraryAction.Type.REMOVE) continue;
                    if(!libraryAction.fileName.equals(entry.getKey())) continue;
                    System.out.println("Nevermind it removed it");
                    nevermind = true;
                    break;
                }
                if(!nevermind) {
                    ServerConnection.INSTANCE.downloadTrack(entry.getKey());
                    changes = true;
                }
            } else if(!matches) {
                //TODO handle
                System.out.println("!!!!! NO MATCH CHECKSUM " + entry.getKey() + " !!!! (server: " + entry.getValue() + ", client: " + clientValue + ")");
                boolean nevermind = false;
                for(LibraryAction libraryAction : actionsSent) {
                    if(libraryAction.actionType != LibraryAction.Type.REPLACE && libraryAction.actionType != LibraryAction.Type.CHANGE_METADATA) continue;
                    if(!libraryAction.fileName.equals(entry.getKey())) continue;
                    System.out.println("Nevermind it replaced it");
                    nevermind = true;
                    break;
                }
            }
        }
        // already checked checksums, now check for missing only
        for(Track track : Library.INSTANCE.tracks) {
            String name = track.getFile().getName();
            if(!hashes.containsKey(name)) {
                System.out.println("Server doesn't have " + name);

                boolean nevermind = false;
                for(LibraryAction libraryAction : actionsSent) {
                    if(libraryAction.actionType != LibraryAction.Type.ADD) continue;
                    if(!libraryAction.fileName.equals(name)) continue;
                    System.out.println("Nevermind it already sent it");
                    nevermind = true;
                    break;
                }
                if(!nevermind) {
                    ServerConnection.INSTANCE.sendAddTrack(name);
                    changes = true;
                }
            }
        }
        if(changes) {
            Library.INSTANCE.reloadAll();
        }
    }

    //@Override
    public static LibraryHashesMessage fromJson(JsonObject json) throws MessageException {
        JsonObject hashes = json.get("hashes").getAsJsonObject();
        Map<String, Long> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : hashes.asMap().entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAsLong());
        }
        return new LibraryHashesMessage(map);
    }
}
