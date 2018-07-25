package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.BuildConfig;
import net.daverix.slingerorm.android.SQLiteDatabaseWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.daverix.slingerorm.android.SqliteDatabaseSubject.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class MultipleKeyEntityTest {
    private SQLiteDatabase db;
    private MultipleKeyEntityStorage sut;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
        sut = SlingerMultipleKeyEntityStorage.builder()
                .database(new SQLiteDatabaseWrapper(db))
                .build();
    }

    @Test
    public void createTableSqlShouldNotThrowException() {
        sut.createTable();
    }

    @Test
    public void tableHasMultiplePrimaryKeys() {
        sut.createTable();

        assertThat(db)
                .withTable("MultipleKeyEntity")
                .hasPrimaryKey("groupId", "userId");
    }
}
