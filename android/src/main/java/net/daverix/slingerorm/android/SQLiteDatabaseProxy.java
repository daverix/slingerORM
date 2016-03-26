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
import android.database.sqlite.SQLiteDatabase;

public class SQLiteDatabaseProxy extends AbstractDatabaseProxy {
    private final SQLiteDatabase db;
    
    public SQLiteDatabaseProxy(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void execSql(String sql) {
        db.execSQL(sql);
    }

    @Override
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    public void insert(String tableName, ContentValues values) {
        db.insertOrThrow(tableName, null, values);
    }

    @Override
    public void replace(String tableName, ContentValues values) {
        db.replaceOrThrow(tableName, null, values);
    }

    @Override
    public int update(String tableName, ContentValues values, String where, String[] whereArgs) {
        return db.update(tableName, values, where, whereArgs);
    }

    @Override
    public int delete(String tableName, String where, String[] whereArgs) {
        return db.delete(tableName, where, whereArgs);
    }

    @Override
    public void beginTransaction() {
        db.beginTransaction();
    }

    @Override
    public void endTransaction() {
        db.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }
}
