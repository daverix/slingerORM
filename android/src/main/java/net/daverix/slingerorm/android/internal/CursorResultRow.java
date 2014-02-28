package net.daverix.slingerorm.android.internal;

import android.database.Cursor;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.ResultRow;

/**
 * Created by daverix on 3/2/14.
 */
public class CursorResultRow implements ResultRow {
    private final Cursor mCursor;

    public CursorResultRow(Cursor cursor) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        mCursor = cursor;
    }

    @Override
    public byte[] getBlob(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getBlob(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public String getString(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getString(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public double getDouble(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getDouble(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public float getFloat(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getFloat(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public boolean getBoolean(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getInt(mCursor.getColumnIndexOrThrow(fieldName)) == 1;
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public int getInt(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getInt(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public short getShort(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getShort(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }

    @Override
    public long getLong(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("field " + fieldName + " not found", e);
        }
    }
}
