package net.daverix.slingerorm.mapping;

public interface ResultRows extends Iterable<ResultRow> {
    /**
     * Must be called when done with the iterator!
     */
    void close();
}
