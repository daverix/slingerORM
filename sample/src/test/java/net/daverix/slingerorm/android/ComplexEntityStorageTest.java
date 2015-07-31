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

package net.daverix.slingerorm.android;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.ComplexEntity;
import net.daverix.slingerorm.android.model.ComplexEntityMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static net.daverix.slingerorm.android.SqliteDatabaseSubject.database;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private SlingerStorage sut;
    private SQLiteDatabase db;

    @Before
    public void before() {
        db = SQLiteDatabase.create(null);
        sut = new SlingerStorage(db);
        sut.registerMapper(ComplexEntity.class, new ComplexEntityMapper());
        sut.createTable(ComplexEntity.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorIfMapperIsNotRegistered() {
        sut.createTable(Void.class);
    }

    @Test
    public void shouldInsertThreeItemsAndGetThemBack() {
        ComplexEntity first = new ComplexEntity(1, "alpha", 1, true);
        ComplexEntity second = new ComplexEntity(2, "beta", 2, true);
        ComplexEntity third = new ComplexEntity(3, "gamma", 3, true);

        sut.beginTransaction();
        try {
            sut.insert(first);
            sut.insert(second);
            sut.insert(third);
            sut.setTransactionSuccessful();
        } finally {
            sut.endTransaction();
        }

        List<ComplexEntity> actual = sut.select(ComplexEntity.class).toList();
        assertThat(actual).containsExactlyElementsIn(Arrays.asList(first, second, third));
    }

    @Test
    public void shouldUpdateItem() {
        ComplexEntity item = new ComplexEntity(1, "alpha", 1, true);
        ComplexEntity updated = new ComplexEntity(1, "alpha2", 2, false);

        sut.insert(item);

        assertThat(sut.select(ComplexEntity.class).where("_id = ?", "1").first()).isEqualTo(item);

        sut.update(updated);

        assertThat(sut.select(ComplexEntity.class).where("_id = ?", "1").first()).isEqualTo(updated);
    }

    @Test
    public void shouldDeleteItem() {
        ComplexEntity item = new ComplexEntity(1, "alpha", 1, true);

        sut.insert(item);

        assertAbout(database()).that(db).withTable("Complex").isNotEmpty();

        sut.delete(item);

        assertAbout(database()).that(db).withTable("Complex").isEmpty();
    }

    @Test
    public void shouldReplaceItem() {
        ComplexEntity item = new ComplexEntity(1, "alpha", 1, true);
        ComplexEntity updated = new ComplexEntity(1, "alpha2", 2, false);

        sut.insert(item);

        assertThat(sut.select(ComplexEntity.class).where("_id = ?", "1").first()).isEqualTo(item);

        sut.replace(updated);

        assertThat(sut.select(ComplexEntity.class).where("_id = ?", "1").first()).isEqualTo(updated);
    }

    @Test
    public void shouldInsertItemWhenCallingReplace() {
        ComplexEntity item = new ComplexEntity(1, "alpha", 2, false);

        sut.replace(item);

        assertThat(sut.select(ComplexEntity.class).where("_id = ?", "1").first()).isEqualTo(item);
    }
}
