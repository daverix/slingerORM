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
        return new ContentValuesContainer(db, tableName);
    }

    @Override
    public DataPointer query(boolean distinct, String tableName, String[] columns, String where,
                             String[] whereArgs, String groupBy, String having, String orderBy,
                             String limit) {
        Cursor cursor = db.query(distinct, tableName, columns, where, whereArgs, groupBy, having, orderBy,
                limit);
        return cursor == null ? null : new CursorDataPointer(cursor);
    }

    private static class ContentValuesContainer implements DataContainer {
        private final ContentValues values = new ContentValues();
        private final SQLiteDatabase db;
        private final String table;

        public ContentValuesContainer(SQLiteDatabase db, String table) {
            this.db = db;
            this.table = table;
        }

        @Override
        public DataContainer put(String key, String value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Byte value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Short value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Integer value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Long value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Float value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Double value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, Boolean value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer put(String key, byte[] value) {
            values.put(key, value);
            return this;
        }

        @Override
        public DataContainer putNull(String key) {
            values.putNull(key);
            return this;
        }

        @Override
        public int update(String where, String[] whereArgs) {
            return db.update(table, values, where, whereArgs);
        }

        @Override
        public long replace() {
            return db.replaceOrThrow(table, null, values);
        }

        @Override
        public long insert() {
            return db.insertOrThrow(table, null, values);
        }
    }

    private static class CursorDataPointer implements DataPointer {
        private final Cursor cursor;

        CursorDataPointer(Cursor cursor) {
            if(cursor == null)
                throw new IllegalArgumentException("cursor is null");

            this.cursor = cursor;
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public int getPosition() {
            return cursor.getPosition();
        }

        @Override
        public boolean move(int offset) {
            return cursor.move(offset);
        }

        @Override
        public boolean moveToPosition(int position) {
            return cursor.moveToPosition(position);
        }

        @Override
        public boolean moveToFirst() {
            return cursor.moveToFirst();
        }

        @Override
        public boolean moveToLast() {
            return cursor.moveToLast();
        }

        @Override
        public boolean moveToNext() {
            return cursor.moveToNext();
        }

        @Override
        public boolean moveToPrevious() {
            return cursor.moveToPrevious();
        }

        @Override
        public boolean isFirst() {
            return cursor.isFirst();
        }

        @Override
        public boolean isBeforeFirst() {
            return cursor.isBeforeFirst();
        }

        @Override
        public boolean isAfterLast() {
            return cursor.isAfterLast();
        }

        @Override
        public int getColumnIndex(String key) {
            return cursor.getColumnIndex(key);
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            return cursor.getBlob(columnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            return cursor.getString(columnIndex);
        }

        @Override
        public short getShort(int columnIndex) {
            return cursor.getShort(columnIndex);
        }

        @Override
        public int getInt(int columnIndex) {
            return cursor.getInt(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            return cursor.getLong(columnIndex);
        }

        @Override
        public float getFloat(int columnIndex) {
            return cursor.getFloat(columnIndex);
        }

        @Override
        public double getDouble(int columnIndex) {
            return cursor.getDouble(columnIndex);
        }

        @Override
        public boolean isNull(int columnIndex) {
            return cursor.isNull(columnIndex);
        }

        @Override
        public void close() {
            cursor.close();
        }
    }
}
