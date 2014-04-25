package net.daverix.slingerorm.exception;

public class FetchMappingException extends Exception {
    public FetchMappingException() {
    }

    public FetchMappingException(String detailMessage) {
        super(detailMessage);
    }

    public FetchMappingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public FetchMappingException(Throwable throwable) {
        super(throwable);
    }
}
