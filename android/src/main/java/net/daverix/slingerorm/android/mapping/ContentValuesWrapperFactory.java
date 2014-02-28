package net.daverix.slingerorm.android.mapping;

import android.content.ContentValues;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class ContentValuesWrapperFactory implements InsertableContentValuesFactory {

    @Inject
    public ContentValuesWrapperFactory() {
    }

    @Override
    public InsertableContentValues create() {
        return new ContentValuesWrapper();
    }

    public class ContentValuesWrapper implements InsertableContentValues {
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
        public void put(String fieldName, Date value) {
            mValues.put(fieldName, value != null ? value.getTime() : 0);
        }

        @Override
        public void put(String fieldName, BigDecimal value) {
            mValues.put(fieldName, value != null ? value.doubleValue() : 0);
        }

        @Override
        public void put(String fieldName, UUID value) {
            mValues.put(fieldName, value != null ? value.toString() : null);
        }

        @Override
        public ContentValues getData() {
            return mValues;
        }
    }
}