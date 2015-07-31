package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;

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

    @Before @SuppressWarnings("unchecked")
    public void before() {
        mapper = mock(Mapper.class);
        databaseProxy = mock(AbstractDatabaseProxy.class);
        sut = new SlingerStorage(databaseProxy);
        sut.registerMapper(TestEntity.class, mapper);
    }

    @Test
    public void shouldCallDatabaseProxyWhenCreatingTable() {
        String sql = "CREATE TABLE TestEntity (id PRIMARY KEY NOT NULL, name TEXT)";
        when(mapper.createTable()).thenReturn(sql);
        sut.createTable(TestEntity.class);

        verify(databaseProxy).execSql(sql);
    }

    @Test
    public void shouldCallDatabaseProxyWhenInserting() {
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
    public void shouldCallDatabaseProxyWhenReplacing() {
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
    public void shouldCallDatabaseProxyWhenUpdating() {
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
    public void shouldCallDatabaseProxyWhenDeleting() {
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
    public void shouldCallDatabaseProxyWhenSelectingList() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        String where = "age > ?";
        String[] whereArgs = new String[] {"42"};
        String having = "count(someId) > 2";
        String groupBy = "age";
        String orderBy ="age DESC";
        String limit = "10";

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getFieldNames()).thenReturn(fields);

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
    public void shouldCallDatabaseProxyWhenSelectingFirst() {
        String tableName = "TestEntity";
        String[] fields = new String[] {"id", "name"};
        String where = "age > ?";
        String[] whereArgs = new String[] {"42"};
        String having = "count(someId) > 2";
        String groupBy = "age";
        String orderBy ="age DESC";
        String limit = "10";

        when(mapper.getTableName()).thenReturn(tableName);
        when(mapper.getFieldNames()).thenReturn(fields);

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
    public void shouldReturnListFromCursor() {
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
        when(mapper.getFieldNames()).thenReturn(fields);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Cursor cursor = (Cursor) invocation.getArguments()[0];
                return expected.get(cursor.getPosition());
            }
        }).when(mapper).mapItem(cursor);
        when(mapper.mapList((Cursor) anyObject())).thenCallRealMethod();
        when(databaseProxy.query(false, tableName, fields, null, null, null, null, null, null)).thenReturn(cursor);

        List<TestEntity> actual = sut.select(TestEntity.class).toList();
        assertThat(actual).containsExactlyElementsIn(expected);
    }

    private class TestEntity {
        int id;
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
