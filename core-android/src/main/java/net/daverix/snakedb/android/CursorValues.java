package net.daverix.snakedb.android;

import android.database.Cursor;

import net.daverix.snakedb.exception.FieldNotFoundException;
import net.daverix.snakedb.mapping.IFetchableValues;

public class CursorValues implements IFetchableValues {
    private final Cursor mCursor;

    public CursorValues(Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public byte[] getBlob(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getBlob(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public String getString(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getString(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public double getDouble(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getDouble(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public float getFloat(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getFloat(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public int getInt(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getInt(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public short getShort(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getShort(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public long getLong(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(fieldName));
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }

    @Override
    public boolean getBoolean(String fieldName) throws FieldNotFoundException {
        try {
            return mCursor.getShort(mCursor.getColumnIndexOrThrow(fieldName)) == 1;
        } catch (IllegalArgumentException e) {
            throw new FieldNotFoundException("Could not find fied " + fieldName);
        }
    }
}
