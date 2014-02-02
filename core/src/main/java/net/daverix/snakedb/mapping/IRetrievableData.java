package net.daverix.snakedb.mapping;

/**
 * Created by daverix on 2/1/14.
 */
public interface IRetrievableData<T> extends IInsertableValues {
    public T getData();
}
