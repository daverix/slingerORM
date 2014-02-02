package net.daverix.snakedb.exception;

/**
 * Created by daverix on 2/1/14.
 */
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
