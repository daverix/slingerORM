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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static net.daverix.slingerorm.android.SqliteDatabaseSubject.database;

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
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue, true);
        entity.setIgnoreThisField("ignore this");

        try {
            db.beginTransaction();
            sut.insert(db, entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        assertAbout(database()).that(db).withTable("Complex").hasRowCountThat().isNotEqualTo(0);

        final ComplexEntity actual = sut.getEntity(db, expectedId);

        assertThat(actual).named("entity").isNotNull();

        assertThat(actual.getId()).named("id").isEqualTo(expectedId);
        assertThat(actual.getEntityName()).named("entityName").isEqualTo(expectedName);
        assertThat(actual.getValue()).named("value").isWithin(0.000001d).of(expectedValue);
        assertThat(actual.getIgnoreThisField()).named("ignore").isNull();
        assertThat(actual.isComplex()).named("complex").isTrue();
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
            sut.insert(db, oldEntity);
            sut.update(db, entity);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final ComplexEntity actual = sut.getEntity(db, expectedId);

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
            sut.insert(db, first);
            sut.insert(db, second);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getAllEntities(db);

        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(actual).containsExactlyElementsIn(Arrays.asList(first, second));
    }

    @Test
    public void ShouldInsertOneComplexAndOneNotComplexAndGetComplexBack() throws Exception {
        final ComplexEntity first = createEntity(42, "Adam", 2, false);
        final ComplexEntity second = createEntity(1337, "David", 3, true);

        try {
            db.beginTransaction();
            sut.insert(db, first);
            sut.insert(db, second);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        final List<ComplexEntity> actual = sut.getComplexEntities(db, true);

        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(actual).containsExactlyElementsIn(Collections.singletonList(second));
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
