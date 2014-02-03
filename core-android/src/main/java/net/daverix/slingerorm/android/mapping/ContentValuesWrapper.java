package net.daverix.slingerorm.android.mapping;

import android.content.ContentValues;

public class ContentValuesWrapper implements IContentValuesWrapper {
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
