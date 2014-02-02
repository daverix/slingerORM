package net.daverix.snakedb.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.snakedb.exception.InitStorageException;
import net.daverix.snakedb.mapping.IFetchableValuesFactory;
import net.daverix.snakedb.mapping.IRetrievableData;
import net.daverix.snakedb.mapping.IRetrievableDataFactory;
import net.daverix.snakedb.IStorage;
import net.daverix.snakedb.exception.FieldNotFoundException;
import net.daverix.snakedb.mapping.IMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage that uses an {@link SQLiteDatabase} and an {@link IMapping} to store and retrieve an
 * instance of a {@link net.daverix.snakedb.annotation.DatabaseEntity} annotated class.
 */
public class SQLiteStorage<T> implements IStorage<T> {
    private final SQLiteDatabase mDb;
    private final IMapping<T> mMapping;
    private final IRetrievableDataFactory<ContentValues> mInsertableValuesFactory;
    private final IFetchableValuesFactory<Cursor> mFetchableValuesFactory;

    public SQLiteStorage(SQLiteDatabase db, IMapping<T> mapping,
                         IRetrievableDataFactory<ContentValues> insertableValuesFactory,
                         IFetchableValuesFactory<Cursor> fetchableValuesFactory) {
        if(db == null) throw new IllegalArgumentException("db is null");
        if(mapping == null) throw new IllegalArgumentException("mapping is null");
        if(insertableValuesFactory == null) throw new IllegalArgumentException("insertableValuesFactory is null");
        if(fetchableValuesFactory == null) throw new IllegalArgumentException("fetchableValuesFactory is null");

        mDb = db;
        mMapping = mapping;
        mInsertableValuesFactory = insertableValuesFactory;
        mFetchableValuesFactory = fetchableValuesFactory;
    }

    @Override
    public void initStorage() throws InitStorageException {
        try {
            mDb.execSQL(mMapping.getCreateTableSql());
        } catch (SQLException e) {
            throw new InitStorageException(e);
        }
    }

    @Override
    public void insert(T item) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");

        IRetrievableData<ContentValues> values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        mDb.insert(mMapping.getTableName(), null, values.getData());
    }

    @Override
    public void update(T item) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");

        IRetrievableData<ContentValues> values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        update(item, mMapping.getIdFieldName() + "=?", new String[]{mMapping.getId(item)});
    }

    private void update(T item, String selection, String[] selectionArgs) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");
        if(selection == null) throw new IllegalArgumentException("selection is null");
        if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs is null");

        IRetrievableData<ContentValues> values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        mDb.update(mMapping.getTableName(), values.getData(),selection, selectionArgs);
    }

    @Override
    public void delete(T item) {
        if(item == null) throw new IllegalArgumentException("item is null");

        delete(mMapping.getIdFieldName() + "=?", new String[]{mMapping.getId(item)});
    }

    private void delete(String selection, String[] selectionArgs) {
        if(selection == null) throw new IllegalArgumentException("selection is null");
        if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs is null");

        mDb.delete(mMapping.getTableName(), selection, selectionArgs);
    }

    @Override
    public T get(String id) throws FieldNotFoundException {
        if(id == null) throw new IllegalArgumentException("id is null");

        Cursor cursor = null;

        try {
            cursor = mDb.query(false, mMapping.getTableName(), null,
                    mMapping.getIdFieldName() + "=?", new String[]{id}, null, null, null, null);
            if(cursor == null || !cursor.moveToFirst())
                return null;

            return mMapping.map(mFetchableValuesFactory.create(cursor));
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    @Override
    public List<T> query(String selection,
                         String[] selectionArgs,
                         String orderBy) throws FieldNotFoundException {
        Cursor cursor = null;
        try {
            cursor = mDb.query(false, mMapping.getTableName(), null, selection, selectionArgs,
                    null, null, orderBy, null);
            return getItemsFromCursor(cursor);
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    protected List<T> getItemsFromCursor(Cursor cursor) throws FieldNotFoundException {
        List<T> items = new ArrayList<T>();
        if(cursor != null && cursor.moveToFirst()) {
            do {
                items.add(mMapping.map(mFetchableValuesFactory.create(cursor)));
            } while (cursor.moveToNext());
        }
        return items;
    }
}
