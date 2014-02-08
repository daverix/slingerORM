package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.exception.FetchMappingException;

/**
 * Created by daverix on 2/1/14.
 */
public interface MappingFetcher {
    /**
     * Does a lazy call to get the mapping for a specific entity
     *
     * @param entityClass the entity class
     * @param <T> a type annotated with {@link DatabaseEntity}
     * @return mapping for entity
     * @throws FetchMappingException if mapping can't fetched
     */
    <T> Mapping<T> fetchMapping(Class<T> entityClass) throws FetchMappingException;
}
