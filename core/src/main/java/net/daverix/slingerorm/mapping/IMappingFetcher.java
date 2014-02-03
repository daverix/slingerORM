package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.exception.FetchMappingException;

/**
 * Created by daverix on 2/1/14.
 */
public interface IMappingFetcher {
    void registerEntity(Class<?> entityClass);

    <T> IMapping<T> getMapping(Class<T> entityClass) throws FetchMappingException;

    void initialize() throws FetchMappingException;
}
