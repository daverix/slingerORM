package net.daverix.slingerorm.android.test.dagger;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.dagger.MappingModule;
import net.daverix.slingerorm.android.storage.SQLiteDatabaseReference;
import net.daverix.slingerorm.storage.EntityStorageFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by daverix on 2/8/14.
 */
@Module(includes = MappingModule.class, injects = EntityStorageFactory.class, overrides = true)
public class MappingTestModule {
    @Singleton @Provides
    public SQLiteDatabaseReference provideSQLiteDatabaseReference() {
        SQLiteDatabase db = SQLiteDatabase.create(null);
        SQLiteDatabaseReference dbRef = mock(SQLiteDatabaseReference.class);
        when(dbRef.getReadableDatabase()).thenReturn(db);
        when(dbRef.getWritableDatabase()).thenReturn(db);

        return dbRef;
    }
}
