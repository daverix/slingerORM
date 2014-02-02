package net.daverix.snakedb.mapping;

import net.daverix.snakedb.exception.FetchMappingException;
import net.daverix.snakedb.mapping.IMapping;

/**
 * Created by daverix on 2/1/14.
 */
public interface IMappingFetcher {
    void registerEntity(Class<?> entityClass);

    <T> IMapping<T> getMapping(Class<T> entityClass) throws FetchMappingException;

    void initialize() throws FetchMappingException;
}
