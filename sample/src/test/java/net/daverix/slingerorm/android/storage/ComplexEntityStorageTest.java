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
package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.ComplexEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private SQLiteDatabase db;
    private ComplexEntityStorage sut;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
        sut = SlingerComplexEntityStorage.builder().build();
        sut.createTable(db);
    }

    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);
        entity.setIgnoreThisField("ignore this");

        try {
            db.beginTransaction();
            sut.insert(db, entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getComplexEntities(db, true);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(not(equalTo(0))));
        assertThat(actual.get(0).getId(), is(equalTo(expectedId)));
        assertThat(actual.get(0).getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.get(0).getValue(), is(equalTo(expectedValue)));
        assertThat(actual.get(0).getIgnoreThisField(), is(nullValue()));
    }

    @Test
    public void shouldSaveAndUpdateObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity oldEntity = createEntity(expectedId, "Adam", 2);
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);

        try {
            db.beginTransaction();
            sut.insert(db, oldEntity);
            sut.update(db, entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getComplexEntities(db, true);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(not(equalTo(0))));
        assertThat(actual.get(0).getId(), is(equalTo(expectedId)));
        assertThat(actual.get(0).getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.get(0).getValue(), is(equalTo(expectedValue)));
        assertThat(actual.get(0).getIgnoreThisField(), is(nullValue()));
    }

    private ComplexEntity createEntity(long id, String name, double value) {
        ComplexEntity entity =new ComplexEntity();
        entity.setId(id);
        entity.setEntityName(name);
        entity.setValue(value);
        return entity;
    }
}
