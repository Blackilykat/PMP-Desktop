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
