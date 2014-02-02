package net.daverix.snakedb.mapping;

/**
 * Created by daverix on 2/1/14.
 */
public interface IFetchableValuesFactory<T> {
    public IFetchableValues create(T args);
}
