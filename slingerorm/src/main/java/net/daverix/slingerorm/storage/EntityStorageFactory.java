package net.daverix.slingerorm.storage;

import net.daverix.slingerorm.exception.FetchMappingException;
import net.daverix.slingerorm.exception.InitStorageException;

/**
 * Created by daverix on 2/8/14.
 */
public interface EntityStorageFactory {
    public <T> EntityStorage<T> getStorage(Class<T> entityClass) throws FetchMappingException, InitStorageException;
}
