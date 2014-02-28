package net.daverix.slingerorm.exception;

/**
 * Created by daverix on 3/2/14.
 */
public class TypeSerializationException extends Exception {
    public TypeSerializationException() {
    }

    public TypeSerializationException(String message) {
        super(message);
    }

    public TypeSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeSerializationException(Throwable cause) {
        super(cause);
    }

    public TypeSerializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
