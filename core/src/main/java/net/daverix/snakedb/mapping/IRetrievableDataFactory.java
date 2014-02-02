package net.daverix.snakedb.mapping;

/**
 * Created by daverix on 2/1/14.
 */
public interface IRetrievableDataFactory<T> {
    public IRetrievableData<T> create();
}
