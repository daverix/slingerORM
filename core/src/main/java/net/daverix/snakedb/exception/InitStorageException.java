package net.daverix.snakedb.exception;

/**
 * Created by daverix on 2/2/14.
 */
public class InitStorageException extends Exception {
    public InitStorageException() {
    }

    public InitStorageException(String detailMessage) {
        super(detailMessage);
    }

    public InitStorageException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InitStorageException(Throwable throwable) {
        super(throwable);
    }
}
