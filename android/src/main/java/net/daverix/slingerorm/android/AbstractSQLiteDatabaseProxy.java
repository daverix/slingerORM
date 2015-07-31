package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class AbstractSQLiteDatabaseProxy extends AbstractDatabaseProxy {
    private SQLiteDatabase db;

    public abstract SQLiteDatabase createDatabase();

    protected SQLiteDatabase getDatabase() {
        if(db == null) db = createDatabase();

        return db;
    }

    @Override
    public void execSql(String sql) {
        getDatabase().execSQL(sql);
    }

    @Override
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return getDatabase().query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    public void insert(String tableName, ContentValues values) {
        getDatabase().insertOrThrow(tableName, null, values);
    }

    @Override
    public void replace(String tableName, ContentValues values) {
        getDatabase().replace(tableName, null, values);
    }

    @Override
    public int update(String tableName, ContentValues values, String where, String[] whereArgs) {
        return getDatabase().update(tableName, values, where, whereArgs);
    }

    @Override
    public int delete(String tableName, String where, String[] whereArgs) {
        return getDatabase().delete(tableName, where, whereArgs);
    }

    @Override
    public void beginTransaction() {
        getDatabase().beginTransaction();
    }

    @Override
    public void endTransaction() {
        getDatabase().endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        getDatabase().setTransactionSuccessful();
    }
}
