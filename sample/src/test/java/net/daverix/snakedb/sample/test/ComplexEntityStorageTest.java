package net.daverix.snakedb.sample.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.snakedb.IStorage;
import net.daverix.snakedb.android.CursorValuesFactory;
import net.daverix.snakedb.android.DatabaseValuesFactory;
import net.daverix.snakedb.mapping.IFetchableValuesFactory;
import net.daverix.snakedb.mapping.IRetrievableDataFactory;
import net.daverix.snakedb.android.SQLiteStorage;
import net.daverix.snakedb.mapping.IMapping;
import net.daverix.snakedb.mapping.IMappingFetcher;
import net.daverix.snakedb.mapping.MappingFetcher;
import net.daverix.snakedb.sample.ComplexEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by daverix on 2/2/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private IStorage<ComplexEntity> mStorage;
    
    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);
        
        mStorage.insert(entity);
        final ComplexEntity actual = mStorage.get(String.valueOf(expectedId));

        assertThat(actual.getId(), is(equalTo(expectedId)));
        assertThat(actual.getName(), is(equalTo(expectedName)));
        assertThat(actual.getValue(), is(equalTo(expectedValue)));
    }

    @Before
    public void setUp() throws Exception {
        SQLiteDatabase db = SQLiteDatabase.create(null);

        IMappingFetcher mappingFetcher = new MappingFetcher();
        mappingFetcher.registerEntity(ComplexEntity.class);
        mappingFetcher.initialize();

        IMapping<ComplexEntity> mapping = mappingFetcher.getMapping(ComplexEntity.class);

        IRetrievableDataFactory<ContentValues> retrievableDataFactory = new DatabaseValuesFactory();

        IFetchableValuesFactory<Cursor> fetchableValuesFactory = new CursorValuesFactory();

        mStorage = new SQLiteStorage<ComplexEntity>(db, mapping, retrievableDataFactory, fetchableValuesFactory);
        mStorage.initStorage();
    }

    private ComplexEntity createEntity(long id, String name, double value) {
        ComplexEntity entity =new ComplexEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setValue(value);
        return entity;
    }
}
