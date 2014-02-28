package net.daverix.slingerorm.android.test.dagger;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.DatabaseConnection;
import net.daverix.slingerorm.SessionFactory;
import net.daverix.slingerorm.android.SQLiteDatabaseConnection;
import net.daverix.slingerorm.android.dagger.MappingModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by daverix on 2/8/14.
 */
@Module(includes = MappingModule.class, injects = SessionFactory.class, overrides = true)
public class MappingTestModule {

    @Provides @Singleton
    public SQLiteDatabase provideSQLiteDatabase() {
        return SQLiteDatabase.create(null);
    }

    @Provides @Singleton
    public DatabaseConnection provideConnection(SQLiteDatabaseConnection connection) {
        return connection;
    }
}
