package net.daverix.snakedb.android;

import android.content.ContentValues;

import net.daverix.snakedb.mapping.IRetrievableData;

public class DatabaseValues implements IRetrievableData<ContentValues> {
    private final ContentValues mValues = new ContentValues();

    @Override
    public void put(String fieldName, byte[] value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, String value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, double value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, float value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, int value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, short value) {
        mValues.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, long value) {
        mValues.put(fieldName, value);
    }

    @Override
    public ContentValues getData() {
        return mValues;
    }
}
