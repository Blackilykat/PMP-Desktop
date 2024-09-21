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
