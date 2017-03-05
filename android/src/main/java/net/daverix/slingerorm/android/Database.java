package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;

public interface Database {
    void execSQL(String sql);

    int delete(String tableName, String where, String[] whereArgs);

    Cursor query(boolean distinct, String tableName, String[] columns, String where,
                 String[] whereArgs, String groupBy, String having, String orderBy, String limit);

    int update(String tableName, ContentValues contentValues, String where, String[] whereArgs);

    long replace(String tableName, ContentValues contentValues);

    long insert(String tableName, ContentValues contentValues);
}
