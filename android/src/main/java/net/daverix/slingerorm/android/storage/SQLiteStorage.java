package net.daverix.slingerorm.android.storage;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.storage.EntityStorage;
import net.daverix.slingerorm.android.mapping.FetchableCursorValuesFactory;
import net.daverix.slingerorm.android.mapping.InsertableContentValues;
import net.daverix.slingerorm.android.mapping.InsertableContentValuesFactory;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.exception.InitStorageException;
import net.daverix.slingerorm.mapping.Mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage that uses an {@link SQLiteDatabase} and an {@link net.daverix.slingerorm.mapping.Mapping} to store and retrieve an
 * instance of a {@link net.daverix.slingerorm.annotation.DatabaseEntity} annotated class.
 */
public class SQLiteStorage<T> implements EntityStorage<T> {
    private final SQLiteDatabaseReference mDbReference;
    private final Mapping<T> mMapping;
    private final InsertableContentValuesFactory mInsertableValuesFactory;
    private final FetchableCursorValuesFactory mFetchableCursorValuesFactory;

    public SQLiteStorage(SQLiteDatabaseReference dbReference, Mapping<T> mapping,
                         InsertableContentValuesFactory insertableValuesFactory,
                         FetchableCursorValuesFactory fetchableCursorValuesFactory) {
        if(dbReference == null) throw new IllegalArgumentException("dbReference is null");
        if(mapping == null) throw new IllegalArgumentException("mapping is null");
        if(insertableValuesFactory == null) throw new IllegalArgumentException("insertableValuesFactory is null");
        if(fetchableCursorValuesFactory == null) throw new IllegalArgumentException("fetchableCursorValuesFactory is null");

        mDbReference = dbReference;
        mMapping = mapping;
        mInsertableValuesFactory = insertableValuesFactory;
        mFetchableCursorValuesFactory = fetchableCursorValuesFactory;
    }

    @Override
    public void initStorage() throws InitStorageException {
        try {
            SQLiteDatabase db = mDbReference.getWritableDatabase();
            db.execSQL(mMapping.getCreateTableSql());
        } catch (SQLException e) {
            throw new InitStorageException(e);
        }
    }

    @Override
    public void insert(T item) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");

        InsertableContentValues values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        db.insert(mMapping.getTableName(), null, values.getData());
    }

    @Override
    public void insert(List<T> items) throws FieldNotFoundException {
        if(items == null) throw new IllegalArgumentException("items is null");

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        db.beginTransaction();
        try {
            for(T item : items) {
                insert(item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int update(T item) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");

        InsertableContentValues values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        return update(item, mMapping.getIdFieldName() + "=?", new String[]{mMapping.getId(item)});
    }

    @Override
    public int update(List<T> items) throws FieldNotFoundException {
        if(items == null) throw new IllegalArgumentException("items is null");

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        db.beginTransaction();
        try {
            for(T item : items) {
                update(item);
            }
            db.setTransactionSuccessful();
            return items.size();
        } finally {
            db.endTransaction();
        }
    }

    private int update(T item, String selection, String[] selectionArgs) throws FieldNotFoundException {
        if(item == null) throw new IllegalArgumentException("item is null");
        if(selection == null) throw new IllegalArgumentException("selection is null");
        if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs is null");

        InsertableContentValues values = mInsertableValuesFactory.create();
        mMapping.mapValues(item, values);

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        return db.update(mMapping.getTableName(), values.getData(),selection, selectionArgs);
    }

    @Override
    public int delete(String id) {
        if(id == null) throw new IllegalArgumentException("item is null");

        return delete(mMapping.getIdFieldName() + "=?", new String[]{id});
    }

    @Override
    public int delete(T item) {
        if(item == null) throw new IllegalArgumentException("item is null");

        return delete(mMapping.getIdFieldName() + "=?", new String[]{mMapping.getId(item)});
    }

    @Override
    public int delete(List<String> ids) throws FieldNotFoundException {
        if(ids == null) throw new IllegalArgumentException("ids is null");

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        db.beginTransaction();
        try {
            for(String id : ids) {
                delete(id);
            }
            db.setTransactionSuccessful();
            return ids.size();
        } finally {
            db.endTransaction();
        }
    }

    private int delete(String selection, String[] selectionArgs) {
        if(selection == null) throw new IllegalArgumentException("selection is null");
        if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs is null");

        SQLiteDatabase db = mDbReference.getWritableDatabase();
        return db.delete(mMapping.getTableName(), selection, selectionArgs);
    }

    @Override
    public T get(String id) throws FieldNotFoundException {
        if(id == null) throw new IllegalArgumentException("id is null");

        Cursor cursor = null;

        try {
            SQLiteDatabase db = mDbReference.getReadableDatabase();
            cursor = db.query(false, mMapping.getTableName(), null,
                    mMapping.getIdFieldName() + "=?", new String[]{id}, null, null, null, null);
            if(cursor == null || !cursor.moveToFirst())
                return null;

            return mMapping.map(mFetchableCursorValuesFactory.create(cursor));
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
            SQLiteDatabase db = mDbReference.getReadableDatabase();
            cursor = db.query(false, mMapping.getTableName(), null, selection, selectionArgs,
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
                items.add(mMapping.map(mFetchableCursorValuesFactory.create(cursor)));
            } while (cursor.moveToNext());
        }
        return items;
    }
}
