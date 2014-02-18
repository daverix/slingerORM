package net.daverix.slingerorm.android.mapping;
import android.database.Cursor;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.FetchableValues;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class CursorWrapperFactory implements FetchableCursorValuesFactory {

    @Inject
    public CursorWrapperFactory() {
    }

    @Override
    public FetchableValues create(Cursor cursor) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");

        return new CursorWrapper(cursor);
    }

    public class CursorWrapper implements FetchableValues {
        private final Cursor mCursor;

        public CursorWrapper(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public byte[] getBlob(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getBlob(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public String getString(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getString(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public double getDouble(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getDouble(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public float getFloat(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getFloat(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public int getInt(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getInt(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public short getShort(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getShort(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public long getLong(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getLong(mCursor.getColumnIndexOrThrow(fieldName));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public boolean getBoolean(String fieldName) throws FieldNotFoundException {
            try {
                return mCursor.getShort(mCursor.getColumnIndexOrThrow(fieldName)) == 1;
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public BigDecimal getBigDecimal(String fieldName) throws FieldNotFoundException {
            try {
                return new BigDecimal(mCursor.getDouble(mCursor.getColumnIndexOrThrow(fieldName)));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public Date getDate(String fieldName) throws FieldNotFoundException {
            try {
                return new Date(mCursor.getLong(mCursor.getColumnIndexOrThrow(fieldName)));
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }

        @Override
        public UUID getUUID(String fieldName) throws FieldNotFoundException {
            try {
                String uuid = mCursor.getString(mCursor.getColumnIndexOrThrow(fieldName));
                return uuid != null ? UUID.fromString(uuid) : null;
            } catch (IllegalArgumentException e) {
                throw new FieldNotFoundException("Could not find field " + fieldName);
            }
        }
    }
}
