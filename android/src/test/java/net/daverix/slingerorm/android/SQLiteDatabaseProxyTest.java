package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SQLiteDatabaseProxyTest {
    private SQLiteDatabase db;
    private AbstractDatabaseProxy sut;

    @Before
    public void setUp() {
        db = mock(SQLiteDatabase.class);
        sut = new SQLiteDatabaseProxy(db);
    }

    @Test
    public void execSql_verifyProxyCalled() {
        String sql = "SELECT * FROM Something";
        sut.execSql(sql);

        verify(db).execSQL(sql);
    }

    @Test
    public void beginTransaction_verifyProxyCalled() {
        sut.beginTransaction();

        verify(db).beginTransaction();
    }

    @Test
    public void setTransactionSuccessful_verifyProxyCalled() {
        sut.setTransactionSuccessful();

        verify(db).setTransactionSuccessful();
    }

    @Test
    public void endTransaction_verifyProxyCalled() {
        sut.endTransaction();

        verify(db).endTransaction();
    }

    @Test
    public void insert_verifyProxyCalled() {
        String table = "MyTable";
        ContentValues values = mock(ContentValues.class);

        sut.insert(table, values);

        verify(db).insertOrThrow(table, null, values);
    }

    @Test
    public void replace_verifyProxyCalled() {
        String table = "MyTable";
        ContentValues values = mock(ContentValues.class);

        sut.replace(table, values);

        verify(db).replaceOrThrow(table, null, values);
    }

    @Test
    public void update_verifyProxyCalled() {
        String table = "MyTable";
        ContentValues values = mock(ContentValues.class);
        String where = "id = ?";
        String[] whereArgs = new String[]{"123"};

        sut.update(table, values, where, whereArgs);

        verify(db).update(table, values, where, whereArgs);
    }

    @Test
    public void delete_verifyProxyCalled() {
        String table = "MyTable";
        String where = "id = ?";
        String[] whereArgs = new String[]{"123"};

        sut.delete(table, where, whereArgs);

        verify(db).delete(table, where, whereArgs);
    }

    @Test
    public void query_verifyProxyCalled() {
        String table = "MyTable";
        String[] columns = new String[]{"id", "count(age > 18)"};
        String selection = "groupId = ?";
        String[] selectionArgs = new String[] {"3"};
        String having = "count(something) > 10";
        String groupBy = "id";
        String orderBy = "age";
        String limit = "100";

        sut.query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

        verify(db).query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }
}