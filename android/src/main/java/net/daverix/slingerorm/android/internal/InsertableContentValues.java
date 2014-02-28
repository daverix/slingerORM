package net.daverix.slingerorm.android.internal;

import android.content.ContentValues;

import net.daverix.slingerorm.mapping.InsertableValues;

import java.util.Date;

public class InsertableContentValues implements InsertableValues {
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
    public void put(String fieldName, boolean value) {
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
