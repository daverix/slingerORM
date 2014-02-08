package net.daverix.slingerorm.android.storage;

import net.daverix.slingerorm.android.mapping.FetchableCursorValuesFactory;
import net.daverix.slingerorm.android.mapping.InsertableContentValuesFactory;
import net.daverix.slingerorm.exception.FetchMappingException;
import net.daverix.slingerorm.exception.InitStorageException;
import net.daverix.slingerorm.mapping.Mapping;
import net.daverix.slingerorm.mapping.MappingFetcher;
import net.daverix.slingerorm.storage.EntityStorage;
import net.daverix.slingerorm.storage.EntityStorageFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by daverix on 2/8/14.
 */
public class SQLiteStorageFactory implements EntityStorageFactory {
    private final Map<Class<?>, EntityStorage<?>> mInitiatedStorages = new HashMap<Class<?>, EntityStorage<?>>();
    private final SQLiteDatabaseReference mDbReference;
    private final MappingFetcher mMappingFetcher;
    private final InsertableContentValuesFactory mInsertableContentValuesFactory;
    private final FetchableCursorValuesFactory mFetchableCursorValuesFactory;

    @Inject
    public SQLiteStorageFactory(SQLiteDatabaseReference dbReference,
                                MappingFetcher mappingFetcher,
                                InsertableContentValuesFactory insertableContentValuesFactory,
                                FetchableCursorValuesFactory fetchableCursorValuesFactory) {
        mDbReference = dbReference;
        mMappingFetcher = mappingFetcher;
        mInsertableContentValuesFactory = insertableContentValuesFactory;
        mFetchableCursorValuesFactory = fetchableCursorValuesFactory;
    }

    @Override
    public <T> EntityStorage<T> getStorage(Class<T> entityClass) throws FetchMappingException, InitStorageException {
        EntityStorage<T> storage = getExistingStorage(entityClass);

        if(storage == null) {
            Mapping<T> mapping = mMappingFetcher.fetchMapping(entityClass);
            storage = new SQLiteStorage<T>(mDbReference,
                    mapping,
                    mInsertableContentValuesFactory,
                    mFetchableCursorValuesFactory);
            storage.initStorage();
            putStorage(entityClass, storage);
        }

        return storage;
    }

    @SuppressWarnings("unchecked")
    protected <T> EntityStorage<T> getExistingStorage(Class<T> entityClass) {
        return (EntityStorage<T>) mInitiatedStorages.get(entityClass);
    }

    protected <T> void putStorage(Class<T> entityClass, EntityStorage<T> storage) {
        mInitiatedStorages.put(entityClass, storage);
    }
}
