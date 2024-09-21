package dev.blackilykat.messages.exceptions;

public class MessageMissingContentsException extends MessageException {
    public MessageMissingContentsException() {
        super();
    }

    public MessageMissingContentsException(String message) {
        super(message);
    }

    public MessageMissingContentsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageMissingContentsException(Throwable cause) {
        super(cause);
    }
}
