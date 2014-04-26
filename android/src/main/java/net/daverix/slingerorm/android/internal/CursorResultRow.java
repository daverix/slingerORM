/*
 * Copyright 2014 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.daverix.slingerorm.android.internal;

import android.database.Cursor;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.ResultRow;

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
