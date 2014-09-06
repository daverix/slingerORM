/*
 * Copyright 2014 David Laurell
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
import net.daverix.slingerorm.DatabaseConnection;
import net.daverix.slingerorm.android.internal.CursorResultsFactory;
import net.daverix.slingerorm.android.internal.InsertableContentValues;
import net.daverix.slingerorm.android.internal.ResultRowsFactory;
import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.ResultRows;

import javax.inject.Inject;

/**
 * Created by daverix on 3/1/14.
 */
public class SQLiteDatabaseConnection implements DatabaseConnection {
    private final SQLiteDatabase mDb;
    private final ResultRowsFactory mResultRowsFactory;

    @Inject
    public SQLiteDatabaseConnection(SQLiteDatabase db, ResultRowsFactory resultRowsFactory) {
        if(db == null) throw new IllegalArgumentException("db is null");
        if(resultRowsFactory == null) throw new IllegalArgumentException("resultRowsFactory is null");

        mDb = db;
        mResultRowsFactory = resultRowsFactory;
    }

    public SQLiteDatabaseConnection(SQLiteDatabase db) {
        if(db == null) throw new IllegalArgumentException("db is null");

        mDb = db;
        mResultRowsFactory = new CursorResultsFactory();
    }

    @Override
    public void beginTransaction() {
        mDb.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        mDb.endTransaction();
    }

    @Override
    public void execSql(String sql) {
        if(sql == null) throw new IllegalArgumentException("sql is null");

        mDb.execSQL(sql);
    }

    @Override
    public void execSql(String sql, String[] args) {
        if(sql == null) throw new IllegalArgumentException("sql is null");

        mDb.execSQL(sql, args);
    }

    @Override
    public InsertableValues createValues() {
        return new InsertableContentValues();
    }

    @Override
    public boolean replace(String tableName, InsertableValues values) {
        if(tableName == null) throw new IllegalArgumentException("tableName is null");
        if(values == null) throw new IllegalArgumentException("values is null");

        return mDb.replaceOrThrow(tableName, null, (ContentValues) values.getData()) != -1;
    }

    @Override
    public boolean insert(String tableName, InsertableValues values) {
        if(tableName == null) throw new IllegalArgumentException("tableName is null");
        if(values == null) throw new IllegalArgumentException("values is null");

        return mDb.insertOrThrow(tableName, null, (ContentValues) values.getData()) != -1;
    }

    @Override
    public int update(String tableName, InsertableValues values, String selection, String[] selectionArgs) {
        if(tableName == null) throw new IllegalArgumentException("tableName is null");
        if(values == null) throw new IllegalArgumentException("values is null");

        return mDb.update(tableName, (ContentValues) values.getData(), selection, selectionArgs);
    }

    @Override
    public int delete(String tableName, String selection, String[] selectionArgs) {
        if(tableName == null) throw new IllegalArgumentException("tableName is null");

        return mDb.delete(tableName, selection, selectionArgs);
    }

    @Override
    public ResultRows query(boolean distinct, String tableName, String[] fields, String selection, String[] selectionArgs, String having, String groupBy, String orderBy) {
        if(tableName == null) throw new IllegalArgumentException("tableName is null");

        Cursor cursor = mDb.query(distinct, tableName, fields, selection, selectionArgs, groupBy, having, orderBy, null);
        return mResultRowsFactory.create(cursor);
    }

    @Override
    public void close() {
        mDb.close();
    }
}
