package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import net.daverix.slingerorm.DatabaseConnection;
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
        mDb = db;
        mResultRowsFactory = resultRowsFactory;
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
        mDb.execSQL(sql);
    }

    @Override
    public void execSql(String sql, String[] args) {
        mDb.execSQL(sql, args);
    }

    @Override
    public InsertableValues createValues() {
        return new InsertableContentValues();
    }

    @Override
    public boolean replace(String tableName, InsertableValues values) {
        return mDb.replaceOrThrow(tableName, null, (ContentValues) values.getData()) != -1;
    }

    @Override
    public boolean insert(String tableName, InsertableValues values) {
        return mDb.insertOrThrow(tableName, null, (ContentValues) values.getData()) != -1;
    }

    @Override
    public int update(String tableName, InsertableValues values, String selection, String[] selectionArgs) {
        return mDb.update(tableName, (ContentValues) values.getData(), selection, selectionArgs);
    }

    @Override
    public int delete(String tableName, String selection, String[] selectionArgs) {
        return mDb.delete(tableName, selection, selectionArgs);
    }

    @Override
    public ResultRows query(boolean distinct, String tableName, String[] fields, String selection, String[] selectionArgs, String having, String groupBy, String orderBy) {
        Cursor cursor = mDb.query(distinct, tableName, fields, selection, selectionArgs, groupBy, having, orderBy, null);
        return mResultRowsFactory.create(cursor);
    }

    @Override
    public void close() {
        mDb.close();
    }
}
