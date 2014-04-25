package net.daverix.slingerorm.exception;

public class FieldNotFoundException extends Exception {
    public FieldNotFoundException() {
    }

    public FieldNotFoundException(String detailMessage) {
        super(detailMessage);
    }

    public FieldNotFoundException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public FieldNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
