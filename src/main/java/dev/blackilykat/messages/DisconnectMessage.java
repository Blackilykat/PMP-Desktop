package dev.blackilykat.messages;

import com.google.gson.JsonObject;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.messages.exceptions.MessageException;

/**
 * Used to indicate that a side is disconnecting from the socket. The side receiving this message can expect the side
 * who sent it to have already disconnected from the socket.
 */
public class DisconnectMessage extends Message {
    public static final String MESSAGE_TYPE = "DISCONNECT";
    /**
     * Interval in which the client will attempt to reconnect in seconds. If negative, it will not be included in the
     * json message which indicates the client should not attempt to automatically reconnect.
     */
    public int reconnectIn = -1;

    public DisconnectMessage() {}

    public DisconnectMessage(int reconnectIn) {
        this.reconnectIn = reconnectIn;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        if(reconnectIn >= 0) {
            object.addProperty("reconnect_in", reconnectIn);
        }
    }

    @Override
    public void handle(ServerConnection connection) {
        connection.disconnect();
    }

    //@Override
    public static DisconnectMessage fromJson(JsonObject json) throws MessageException {
        if(json.has("reconnect_in")) {
            return new DisconnectMessage(json.get("reconnect_in").getAsInt());
        } else {
            return new DisconnectMessage();
        }
    }

}
