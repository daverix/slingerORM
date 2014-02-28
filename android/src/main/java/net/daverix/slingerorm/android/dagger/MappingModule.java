package net.daverix.slingerorm.android.dagger;

import dagger.Module;
import dagger.Provides;
import net.daverix.slingerorm.MappingFetcher;
import net.daverix.slingerorm.SessionFactory;
import net.daverix.slingerorm.SlingerSessionFactory;
import net.daverix.slingerorm.android.internal.CursorResultsFactory;
import net.daverix.slingerorm.android.internal.CursorRowResultFactory;
import net.daverix.slingerorm.android.internal.ResultRowFactory;
import net.daverix.slingerorm.android.internal.ResultRowsFactory;
import net.daverix.slingerorm.mapping.LazyMappingFetcher;

import javax.inject.Singleton;

@Module(library = true, complete = false)
public class MappingModule {
    @Singleton @Provides
    public MappingFetcher provideMappingFetcher(LazyMappingFetcher mappingFetcher) {
        return mappingFetcher;
    }

    @Singleton @Provides
    public ResultRowFactory provideResultRowFactory(CursorRowResultFactory factory) {
        return factory;
    }

    @Singleton @Provides
    public ResultRowsFactory provideResultRowCollectionFactory(CursorResultsFactory cursorRowResultFactory) {
        return cursorRowResultFactory;
    }

    @Singleton @Provides
    public SessionFactory provideSessionFactory(SlingerSessionFactory sessionFactory) {
        return sessionFactory;
    }
}
