package net.daverix.slingerorm.android;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteDatabaseWrapper implements Database {
    private final SQLiteDatabase db;

    public SQLiteDatabaseWrapper(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void execSQL(String sql) {
        db.execSQL(sql);
    }

    @Override
    public int delete(String tableName, String where, String[] whereArgs) {
        return db.delete(tableName, where, whereArgs);
    }

    @Override
    public Cursor query(boolean distinct, String tableName, String[] columns, String where,
                        String[] whereArgs, String groupBy, String having, String orderBy,
                        String limit) {
        return db.query(distinct, tableName, columns, where, whereArgs, groupBy, having, orderBy,
                limit);
    }

    @Override
    public int update(String tableName, ContentValues contentValues, String where, String[] whereArgs) {
        return db.update(tableName, contentValues, where, whereArgs);
    }

    @Override
    public long replace(String tableName, ContentValues contentValues) {
        return db.replaceOrThrow(tableName, null, contentValues);
    }

    @Override
    public long insert(String tableName, ContentValues contentValues) {
        return db.insertOrThrow(tableName, null, contentValues);
    }
}
