/*
 * Copyright 2014 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.daverix.slingerorm.android.test.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.DatabaseConnection;
import net.daverix.slingerorm.Storage;
import net.daverix.slingerorm.StorageFactory;
import net.daverix.slingerorm.android.SQLiteDatabaseConnection;
import net.daverix.slingerorm.android.test.dagger.MappingTestModule;
import net.daverix.slingerorm.android.test.model.ComplexEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ComplexEntityStorageTest {
    @Inject Storage<ComplexEntity> mStorage;
    private DatabaseConnection mDatabaseConnection;

    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);
        entity.setIgnoreThisField("ignore this");

        try {
            mDatabaseConnection.beginTransaction();
            mStorage.insert(mDatabaseConnection, entity);
            mDatabaseConnection.setTransactionSuccessful();
        } finally {
            mDatabaseConnection.endTransaction();
        }

        final ComplexEntity actual = mStorage.querySingle(mDatabaseConnection, String.valueOf(expectedId));

        assertThat(actual.getId(), is(equalTo(expectedId)));
        assertThat(actual.getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.getValue(), is(equalTo(expectedValue)));
        assertThat(actual.getIgnoreThisField(), is(nullValue()));
    }

    @Test
    public void shouldSaveAndUpdateObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity oldEntity = createEntity(expectedId, "Adam", 2);
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);

        try {
            mDatabaseConnection.beginTransaction();
            mStorage.replace(mDatabaseConnection, oldEntity);
            mStorage.replace(mDatabaseConnection, entity);
            mDatabaseConnection.setTransactionSuccessful();
        } finally {
            mDatabaseConnection.endTransaction();
        }

        final ComplexEntity actual = mStorage.querySingle(mDatabaseConnection, String.valueOf(expectedId));

        assertThat(actual.getId(), is(equalTo(expectedId)));
        assertThat(actual.getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.getValue(), is(equalTo(expectedValue)));
        assertThat(actual.getIgnoreThisField(), is(nullValue()));
    }

    @Before
    public void setUp() throws Exception {
        ObjectGraph og = ObjectGraph.create(new TestModule());
        og.inject(this);

        mDatabaseConnection = new SQLiteDatabaseConnection(SQLiteDatabase.create(null));
        mStorage.createTable(mDatabaseConnection);
    }

    @Module(includes = MappingTestModule.class, injects = ComplexEntityStorageTest.class)
    public class TestModule {
        @Provides @Singleton
        public Storage<ComplexEntity> provideComplexEntityStorage(StorageFactory storageFactory) {
            return storageFactory.build(ComplexEntity.class);
        }
    }

    private ComplexEntity createEntity(long id, String name, double value) {
        ComplexEntity entity =new ComplexEntity();
        entity.setId(id);
        entity.setEntityName(name);
        entity.setValue(value);
        return entity;
    }
}
