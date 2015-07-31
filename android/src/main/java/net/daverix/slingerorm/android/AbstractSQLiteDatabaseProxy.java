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
