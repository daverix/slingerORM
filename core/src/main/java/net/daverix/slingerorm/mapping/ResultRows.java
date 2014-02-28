package net.daverix.slingerorm.mapping;

/**
 * Created by daverix on 3/2/14.
 */
public interface ResultRows extends Iterable<ResultRow> {
    /**
     * Must be called when done with the iterator!
     */
    void close();
}
