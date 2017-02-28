/*
 * Copyright 2015 David Laurell
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
import net.daverix.slingerorm.core.android.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static net.daverix.slingerorm.android.SqliteDatabaseSubject.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private SQLiteDatabase db;
    private ComplexEntityStorage sut;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
        sut = SlingerComplexEntityStorage.builder()
                .database(db)
                .build();
        sut.createTable();
    }

    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue, true);
        entity.setIgnoreThisField("ignore this");

        try {
            db.beginTransaction();
            sut.insert(entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        assertThat(db).withTable("Complex").isNotEmpty();

        final ComplexEntity actual = sut.getEntity(expectedId);

        assertThat(actual).named("entity").isNotNull();

        assertThat(actual.getId()).named("id").isEqualTo(expectedId);
        assertThat(actual.getEntityName()).named("entityName").isEqualTo(expectedName);
        assertThat(actual.getValue()).named("value").isWithin(0.000001d).of(expectedValue);
        assertThat(actual.getIgnoreThisField()).named("ignore").isNull();
        assertThat(actual.isComplex()).named("complex").isTrue();
    }

    @Test
    public void shouldInsertAndRemoveItem() throws Exception {
        final long expectedId = 42;
        final ComplexEntity entity = createEntity(expectedId, "david", 2, true);

        sut.insert(entity);
        assertThat(db).withTable("Complex").isNotEmpty();

        sut.delete(entity);
        assertThat(db).withTable("Complex").isEmpty();
    }

    @Test
    public void shouldInsertAndUpdateObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity oldEntity = createEntity(expectedId, "Adam", 2, false);
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue, true);

        try {
            db.beginTransaction();
            sut.insert(oldEntity);
            sut.update(entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final ComplexEntity actual = sut.getEntity(expectedId);

        assertThat(actual).named("entity").isNotNull();
        assertThat(actual.getId()).named("id").isEqualTo(expectedId);
        assertThat(actual.getEntityName()).named("entityName").isEqualTo(expectedName);
        assertThat(actual.getValue()).named("value").isWithin(0.000001d).of(expectedValue);
        assertThat(actual.getIgnoreThisField()).named("ignore").isNull();
        assertThat(actual.isComplex()).named("complex").isTrue();
    }

    @Test
    public void ShouldInsertTwoItemsAndGetThemBack() throws Exception {
        final ComplexEntity first = createEntity(42, "Adam", 2, false);
        final ComplexEntity second = createEntity(1337, "David", 3, true);

        try {
            db.beginTransaction();
            sut.insert(first);
            sut.insert(second);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getAllEntities();

        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(actual).containsExactly(first, second);
    }

    @Test
    public void ShouldInsertOneComplexAndOneNotComplexAndGetComplexBack() throws Exception {
        final ComplexEntity first = createEntity(42, "Adam", 2, false);
        final ComplexEntity second = createEntity(1337, "David", 3, true);

        try {
            db.beginTransaction();
            sut.insert(first);
            sut.insert(second);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getComplexEntities(true);

        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(actual).containsExactly(second);
    }

    private ComplexEntity createEntity(long id, String name, double value, boolean complex) {
        ComplexEntity entity = new ComplexEntity();
        entity.setId(id);
        entity.setEntityName(name);
        entity.setValue(value);
        entity.setComplex(complex);
        return entity;
    }
}
