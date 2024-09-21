package dev.blackilykat.messages;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blackilykat.Json;
import dev.blackilykat.Library;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.exceptions.MessageException;

import java.util.HashMap;
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
        boolean changes = false;
        for(Map.Entry<String, Long> entry : hashes.entrySet()) {
            boolean found = false;
            boolean matches = false;
            for(Library.Track track : Library.INSTANCE.tracks) {
                if(track.getFile().getName().equals(entry.getKey())) {
                    found = true;
                    if(track.checksum == entry.getValue()) {
                        matches = true;
                    }
                    break;
                }
            }
            if(!found) {
                System.out.println("Client doesnt have " + entry.getKey());
                ServerConnection.INSTANCE.downloadTrack(entry.getKey());
                changes = true;
            } else if(!matches) {
                //TODO handle
                System.out.println("!!!!! NO MATCH CHECKSUM " + entry.getKey() + " !!!!");
            }
        }
        // already checked checksums, now check for missing only
        for(Library.Track track : Library.INSTANCE.tracks) {
            String name = track.getFile().getName();
            if(!hashes.containsKey(name)) {
                System.out.println("Server doesn't have " + name);
                ServerConnection.INSTANCE.sendAddTrack(name);
                changes = true;
            }
        }
        if(changes) {
            Library.INSTANCE.reload();
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
