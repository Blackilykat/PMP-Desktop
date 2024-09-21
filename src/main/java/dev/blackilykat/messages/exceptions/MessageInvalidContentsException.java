package dev.blackilykat.messages.exceptions;

public class MessageInvalidContentsException extends MessageException {
    public MessageInvalidContentsException() {
        super();
    }

    public MessageInvalidContentsException(String message) {
        super(message);
    }

    public MessageInvalidContentsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageInvalidContentsException(Throwable cause) {
        super(cause);
    }
}
