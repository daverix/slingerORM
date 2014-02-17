package net.daverix.slingerorm.android.dagger;

import dagger.Module;
import dagger.Provides;
import net.daverix.slingerorm.android.mapping.ContentValuesWrapperFactory;
import net.daverix.slingerorm.android.mapping.CursorWrapperFactory;
import net.daverix.slingerorm.android.mapping.FetchableCursorValuesFactory;
import net.daverix.slingerorm.android.mapping.InsertableContentValuesFactory;
import net.daverix.slingerorm.android.storage.SQLiteStorageFactory;
import net.daverix.slingerorm.mapping.LazyMappingFetcher;
import net.daverix.slingerorm.mapping.MappingFetcher;
import net.daverix.slingerorm.storage.EntityStorageFactory;

import javax.inject.Singleton;

/**
 * Created by daverix on 2/8/14.
 */
@Module(library = true, complete = false)
public class MappingModule {
    @Singleton @Provides
    public MappingFetcher provideMappingFetcher(LazyMappingFetcher mappingFetcher) {
        return mappingFetcher;
    }

    @Singleton @Provides
    public EntityStorageFactory provideEntityStorageFactory(SQLiteStorageFactory storageFactory) {
        return storageFactory;
    }

    @Singleton @Provides
    public InsertableContentValuesFactory provideInsertableContentValues(ContentValuesWrapperFactory contentValuesWrapperFactory) {
        return contentValuesWrapperFactory;
    }

    @Singleton @Provides
    public FetchableCursorValuesFactory provideFetchableCursorValuesFactory(CursorWrapperFactory cursorWrapperFactory) {
        return cursorWrapperFactory;
    }
}
