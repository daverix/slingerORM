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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class NativeFieldsMapperTest {
    private Mapper<NativeFieldsEntity> sut;

    @Before
    public void before() {
        sut = new NativeFieldsEntityMapper();
    }

    @Test
    public void shouldGetCorrectTableName() {
        assertThat(sut.getTableName()).isEqualTo("NativeFieldsEntity");
    }

    @Test
    public void shouldSetCorrectContentValues() {
        NativeFieldsEntity entity = new NativeFieldsEntity();
        entity.setTypeBoolean(true);
        entity.setTypeString("hello");
        entity.setTypeDouble(1.23456789010111213d);
        entity.setTypeFloat(1.234567f);
        entity.setTypeLong(1234567891011121314L);
        entity.setTypeInt(1337);
        entity.setTypeShort((short) 42);

        ContentValues values = sut.mapValues(entity);

        assertThat(values.getAsBoolean("typeBoolean")).isTrue();
        assertThat(values.getAsString("typeString")).isEqualTo("hello");
        assertThat(values.getAsDouble("typeDouble")).isWithin(0.000000000000000001).of(1.23456789010111213d);
        assertThat(values.getAsFloat("typeFloat")).isEqualTo(1.234567f);
        assertThat(values.getAsLong("typeLong")).isEqualTo(1234567891011121314L);
        assertThat(values.getAsInteger("typeInt")).isEqualTo(1337);
        assertThat(values.getAsShort("typeShort")).isEqualTo((short) 42);
    }

    @Test
    public void shouldGetCorrectColumnNames() {
        List<String> expected = Arrays.asList("typeBoolean", "typeString", "typeDouble",
                "typeFloat", "typeLong", "typeInt", "typeShort");

        assertThat(sut.getColumnNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        String[] columnNames = new String[]{"typeBoolean", "typeString", "typeDouble", "typeFloat",
                "typeLong", "typeInt", "typeShort"};

        final NativeFieldsEntity expected = new NativeFieldsEntity();
        expected.setTypeBoolean(true);
        expected.setTypeString("hello");
        expected.setTypeDouble(1.23456789010111213d);
        expected.setTypeFloat(1.234567f);
        expected.setTypeLong(1234567891011121314L);
        expected.setTypeInt(1337);
        expected.setTypeShort((short) 42);

        MatrixCursor cursor = new MatrixCursor(columnNames, 1);
        cursor.addRow(createCursorRow(expected));

        cursor.moveToFirst();
        NativeFieldsEntity actual = sut.mapItem(cursor);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldGetListDataFromCursor() {
        String[] columnNames = new String[]{"typeBoolean", "typeString", "typeDouble", "typeFloat",
                "typeLong", "typeInt", "typeShort"};

        final NativeFieldsEntity first = new NativeFieldsEntity();
        first.setTypeBoolean(true);
        first.setTypeString("hello");
        first.setTypeDouble(1.23456789010111213d);
        first.setTypeFloat(1.234567f);
        first.setTypeLong(1234567891011121314L);
        first.setTypeInt(1337);
        first.setTypeShort((short) 42);

        final NativeFieldsEntity second = new NativeFieldsEntity();
        second.setTypeBoolean(false);
        second.setTypeString("hello2");
        second.setTypeDouble(2.23456789010111213d);
        second.setTypeFloat(2.234567f);
        second.setTypeLong(2234567891011121314L);
        second.setTypeInt(2337);
        second.setTypeShort((short) 242);

        MatrixCursor cursor = new MatrixCursor(columnNames, 2);
        cursor.addRow(createCursorRow(first));
        cursor.addRow(createCursorRow(second));

        List<NativeFieldsEntity> actual = sut.mapList(cursor);
        assertThat(actual).containsExactlyElementsIn(Arrays.asList(first, second));
    }

    private Object[] createCursorRow(NativeFieldsEntity entity) {
        return new Object[] {
                (short) (entity.isTypeBoolean() ? 1 : 0),
                entity.getTypeString(),
                entity.getTypeDouble(),
                entity.getTypeFloat(),
                entity.getTypeLong(),
                entity.getTypeInt(),
                entity.getTypeShort()
        };
    }
}
