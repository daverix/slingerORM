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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class SlingerStorageTest {
    private SlingerStorage sut;
    private AbstractDatabaseProxy databaseProxy;
    private Mapper<TestEntity> mapper;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        mapper = mock(Mapper.class);
        databaseProxy = mock(AbstractDatabaseProxy.class);
        sut = new SlingerStorage(databaseProxy);
        sut.registerMapper(TestEntity.class, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerMapper_typeNull_throwIllegalArgumentException() {
        sut.registerMapper(null, mock(Mapper.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void registerMapper_typeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.registerMapper(Object.class, mock(Mapper.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerMapper_mapperNull_throwIllegalArgumentException() {
        sut.registerMapper(TestEntity.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTable_typeNull_throwIllegalArgumentException() {
        sut.createTable(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTable_typeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.createTable(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insert_itemNull_throwIllegalArgumentException() {
        sut.insert(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insert_itemTypeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.insert(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void replace_itemNull_throwIllegalArgumentException() {
        sut.replace(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replace_itemTypeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.replace(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void update_itemNull_throwIllegalArgumentException() {
        sut.update(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void update_itemTypeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.update(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void delete_itemNull_throwIllegalArgumentException() {
        sut.delete(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void delete_itemTypeNotDatabaseEntity_throwIllegalArgumentException() {
        sut.delete(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_typeNull_throwIllegalArgumentException() {
        sut.select(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_typeNotDatabase_throwIllegalArgumentException() {
        sut.select(Object.class);
    }

    @Test
    public void createTable_givenValidEntity_verifyProxyCalled() {
        String sql = "CREATE TABLE TestEntity (id PRIMARY KEY NOT NULL, name TEXT)";
        when(mapper.createTable()).thenReturn(sql);
        sut.createTable(TestEntity.class);

        verify(databaseProxy).execSql(sql);
    }

    @Test
    public void insert_givenValidEntity_verifyProxyCalled() {
        String tableName = "TestEntity";
        when(mapper.getTableName()).thenReturn(tableName);
        ContentValues values = new ContentValues();
        values.put("id", 42);
        values.put("name", "David");
        when(mapper.mapValues((TestEntity) anyObject())).thenReturn(values);
        sut.insert(new TestEntity());

        verify(databaseProxy).insert(tableName, values);
    }

    @Test
    public void replace_givenValidEntity_verifyProxyCalled() {
        String tableName = "TestEntity";
        when(mapper.getTableName()).thenReturn(tableName);
        ContentValues values = new ContentValues();
        values.put("id", 42);
        values.put("name", "David");
        when(mapper.mapValues((TestEntity) anyObject())).thenReturn(values);
        sut.replace(new TestEntity());

        verify(databaseProxy).replace(tableName, values);
    }

    @Test
    public void update_givenValidEntity_verifyProxyCalled() {
        String tableName = "TestEntity";
        String where = "id = ?";
        String[] whereArgs = new String[]{"42"};
        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getItemQuery()).thenReturn(where);
        when(mapper.getItemQueryArguments((TestEntity) anyObject())).thenReturn(whereArgs);
        ContentValues values = new ContentValues();
        values.put("id", 42);
        values.put("name", "David");
        when(mapper.mapValues((TestEntity) anyObject())).thenReturn(values);
        sut.update(new TestEntity());

        verify(databaseProxy).update(tableName, values, where, whereArgs);
    }

    @Test
    public void delete_givenValidEntity_verifyProxyCalled() {
        String tableName = "TestEntity";
        String where = "id = ?";
        String[] whereArgs = new String[]{"42"};
        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getItemQuery()).thenReturn(where);
        when(mapper.getItemQueryArguments((TestEntity) anyObject())).thenReturn(whereArgs);
        sut.delete(new TestEntity());

        verify(databaseProxy).delete(tableName, where, whereArgs);
    }

    @Test
    public void selectWithToList_givenAllChainedMethods_verifyProxyCalled() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        String where = "age > ?";
        String[] whereArgs = new String[] {"42"};
        String having = "count(someId) > 2";
        String groupBy = "age";
        String orderBy ="age DESC";
        String limit = "10";

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getColumnNames()).thenReturn(fields);

        sut.select(TestEntity.class)
                .distinct(true)
                .where(where, whereArgs)
                .having(having)
                .groupBy(groupBy)
                .orderBy(orderBy)
                .limit(limit)
                .toList();

        verify(databaseProxy).query(true, tableName, fields, where, whereArgs, groupBy, having,
                orderBy, limit);
    }

    @Test
    public void selectWithFirst_givenAllChainedMethods_verifyProxyCalled() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        String where = "age > ?";
        String[] whereArgs = new String[] {"42"};
        String having = "count(someId) > 2";
        String groupBy = "age";
        String orderBy ="age DESC";
        String limit = "10";

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getColumnNames()).thenReturn(fields);

        sut.select(TestEntity.class)
                .distinct(true)
                .where(where, whereArgs)
                .having(having)
                .groupBy(groupBy)
                .orderBy(orderBy)
                .limit(limit)
                .first();

        verify(databaseProxy).query(true, tableName, fields, where, whereArgs, groupBy, having,
                orderBy, limit);
    }

    @Test
    public void selectWithToList_givenValidEntityClass_returnMockedItems() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        MatrixCursor cursor = new MatrixCursor(fields, 3);
        cursor.addRow(new Object[]{1, "Alpha"});
        cursor.addRow(new Object[]{2, "Beta"});
        cursor.addRow(new Object[]{3, "Gamma"});
        final List<TestEntity> expected = Arrays.asList(
                new TestEntity(1, "Alpha"),
                new TestEntity(2, "Beta"),
                new TestEntity(3, "Gamma3")
        );

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getColumnNames()).thenReturn(fields);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Cursor cursor = (Cursor) invocation.getArguments()[0];
                return expected.get(cursor.getPosition());
            }
        }).when(mapper).mapItem(cursor);
        when(databaseProxy.query(false, tableName, fields, null, null, null, null, null, null)).thenReturn(cursor);

        List<TestEntity> actual = sut.select(TestEntity.class).toList();
        assertThat(actual).containsExactlyElementsIn(expected);
    }

    @Test
    public void selectWithFirst_givenValidEntityClass_returnFirstItem() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        MatrixCursor cursor = new MatrixCursor(fields, 1);
        cursor.addRow(new Object[]{1, "Alpha"});
        final TestEntity expected = new TestEntity(1, "Alpha");

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getColumnNames()).thenReturn(fields);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return expected;
            }
        }).when(mapper).mapItem(cursor);
        when(databaseProxy.query(false, tableName, fields, null, null, null, null, null, null)).thenReturn(cursor);

        TestEntity actual = sut.select(TestEntity.class).first();
        assertThat(actual).isEqualTo(expected);
    }

    @Test(expected = IllegalStateException.class)
    public void select_givenEntityMapperNotRegistered_throwIllegalStateException() {
        sut.select(MyUnknownDatabaseEntity.class);
    }

    @DatabaseEntity
    private class MyUnknownDatabaseEntity {
        @PrimaryKey String hello;
    }

    @Test
    public void beginTransaction_verifyProxyCalled() {
        sut.beginTransaction();

        verify(databaseProxy).beginTransaction();
    }

    @Test
    public void endTransaction_verifyProxyCalled() {
        sut.endTransaction();

        verify(databaseProxy).endTransaction();
    }

    @Test
    public void setTransactionSuccessful_verifyProxyCalled() {
        sut.setTransactionSuccessful();

        verify(databaseProxy).setTransactionSuccessful();
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_givenSQLiteDatabaseNull_throwIllegalArgumentException() {
        new SlingerStorage((SQLiteDatabase) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_givenProxyNull_throwIllegalArgumentException() {
        new SlingerStorage((AbstractDatabaseProxy) null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void new_givenSQLiteDatabase_verifyProxyCreated() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        Mapper<TestEntity> mapper = mock(Mapper.class);

        String sql = "CREATE TABLE TestEntity";
        when(mapper.createTable()).thenReturn(sql);

        SlingerStorage storage = new SlingerStorage(db);
        storage.registerMapper(TestEntity.class, mapper);

        storage.createTable(TestEntity.class);
        verify(db).execSQL(sql);
    }

    @DatabaseEntity
    public static class TestEntity {
        @PrimaryKey int id;
        String name;

        public TestEntity(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public TestEntity() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestEntity)) return false;

            TestEntity that = (TestEntity) o;

            if (id != that.id) return false;
            return !(name != null ? !name.equals(that.name) : that.name != null);

        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TestEntity{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
