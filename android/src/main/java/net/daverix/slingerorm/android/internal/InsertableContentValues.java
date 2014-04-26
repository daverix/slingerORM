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

import android.content.ContentValues;

import net.daverix.slingerorm.mapping.InsertableValues;

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
