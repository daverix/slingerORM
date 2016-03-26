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

package net.daverix.slingerorm.android.model;

import android.content.ContentValues;
import android.database.MatrixCursor;

import net.daverix.slingerorm.android.Mapper;
import net.daverix.slingerorm.core.android.BuildConfig;
import net.daverix.slingerorm.serialization.DateSerializer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class SerializerEntityMapperTest {
    private Mapper<SerializerEntity> sut;

    @Before
    public void before() {
        sut = new SerializerEntityMapper(new MyObjectSerializer(), new DateSerializer());
    }

    @Test
    public void shouldSetCorrectContentValues() {
        MyObject id = new MyObject("asdasdasd");
        Date created = new Date();
        SerializerEntity entity = new SerializerEntity();
        entity.setId(id);
        entity.setCreated(created);

        ContentValues actual = sut.mapValues(entity);
        assertThat(actual.getAsString("id")).isEqualTo("asdasdasd");
        assertThat(actual.getAsLong("_created")).isEqualTo(created.getTime());
    }

    @Test
    public void shouldGetDataFromCursor() {
        MyObject id = new MyObject("sadsd2323");
        Date created = new Date();
        String[] columnNames = new String[] {"id", "_created"};

        MatrixCursor cursor = new MatrixCursor(columnNames);
        cursor.addRow(new Object[]{"sadsd2323", created.getTime()});

        cursor.moveToFirst();
        SerializerEntity item = sut.mapItem(cursor);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCreated()).isEqualTo(created);
    }
}
