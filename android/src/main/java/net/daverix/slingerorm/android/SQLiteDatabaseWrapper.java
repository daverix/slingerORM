package net.daverix.slingerorm.android;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.DataContainer;
import net.daverix.slingerorm.DataPointer;
import net.daverix.slingerorm.Database;

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
    public DataContainer edit(String tableName) {
        return new ContentValuesContainer(tableName, actions);
    }

    @Override
    public DataPointer query(boolean distinct, String tableName, String[] columns, String where,
                             String[] whereArgs, String groupBy, String having, String orderBy,
                             String limit) {
        Cursor cursor = db.query(distinct, tableName, columns, where, whereArgs, groupBy, having, orderBy,
                limit);
        return cursor == null ? null : new CursorDataPointer(cursor);
    }

    private final ContentValuesContainer.Actions actions = new ContentValuesContainer.Actions() {
        @Override
        public int update(String tableName, ContentValues values, String where, String[] whereArgs) {
            return db.update(tableName, values, where, whereArgs);
        }

        @Override
        public long replace(String tableName, ContentValues values) {
            return db.replaceOrThrow(tableName, null, values);
        }

        @Override
        public long insert(String tableName, ContentValues values) {
            return db.insertOrThrow(tableName, null, values);
        }
    };
}
