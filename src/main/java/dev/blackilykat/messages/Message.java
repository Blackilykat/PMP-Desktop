package dev.blackilykat.messages;

import com.google.gson.JsonObject;
import dev.blackilykat.Json;
import dev.blackilykat.ServerConnection;

public abstract class Message implements Cloneable {
    /**
     * An increasing identifier which should be unique per session (as in every connection will have its own counter)
     */
    public int messageId = -1;

    /**
     * Returns a message identical to this one but with a new {@link #messageId}.
     * Does not change the value on this object.
     * @param messageId the new messageId value
     * @return A copy of the object with the updated message id
     */
    public Message withMessageId(int messageId) {
        Message copy = this.clone();
        copy.messageId = messageId;
        return copy;
    }

    /**
     * @return The type of the message. Should be in all caps, use underscores instead of spaces and be unique.
     */
    public abstract String getMessageType();

    /**
     * Fills the provided {@link JsonObject} with all the information specific to that message type (this means the
     * type itself and the message id are excluded)
     */
    public abstract void fillContents(JsonObject object);

    public abstract void handle(ServerConnection connection);

    public String toJson() {
        if(messageId < 0) {
            throw new IllegalStateException("The message ID is a negative value ("+messageId+")! Did you forget to set it?");
        }
        JsonObject object = new JsonObject();
        object.addProperty("message_type", getMessageType());
        object.addProperty("message_id", messageId);
        fillContents(object);
        return Json.toJson(object);
    }

    /*/*
     * (missing abstract static copium)
     * @param json The json representation of the message
     * @return The message object. This should always be of the same type as the class it is implemented in.
     * @see #withMessageId(int)
     */
    // public abstract static Message fromJson(JsonObject json) throws MessageException;

    /**
     * Creates an identical copy of this message (without the {@link #messageId})
     */
    @Override
    public Message clone() {
        try {
            Message clone = (Message) super.clone();
            clone.messageId = -1;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
