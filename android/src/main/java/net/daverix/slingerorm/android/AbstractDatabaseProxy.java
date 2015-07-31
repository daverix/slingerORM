package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;

public abstract class AbstractDatabaseProxy {
    public abstract void execSql(String sql);

    public abstract Cursor query(boolean distinct, String table, String[] columns,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit);

    public abstract void insert(String tableName, ContentValues values);
    public abstract void replace(String tableName, ContentValues values);
    public abstract int update(String tableName, ContentValues values, String where, String[] whereArgs);
    public abstract int delete(String tableName, String where, String[] whereArgs);

    public abstract void beginTransaction();
    public abstract void endTransaction();
    public abstract void setTransactionSuccessful();
}
