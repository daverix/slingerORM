package net.daverix.slingerorm;

import javax.inject.Inject;

public class SlingerSessionFactory implements SessionFactory {
    private final DatabaseConnection mDatabaseConnection;
    private final MappingFetcher mMappingFetcher;

    @Inject
    public SlingerSessionFactory(DatabaseConnection databaseConnection, MappingFetcher mappingFetcher) {
        mDatabaseConnection = databaseConnection;
        mMappingFetcher = mappingFetcher;
    }

    @Override
    public Session openSession() {
        return new SlingerSession(mDatabaseConnection, mMappingFetcher);
    }
}
