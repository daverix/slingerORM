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

import net.daverix.slingerorm.mapping.ResultRow;

import java.util.Iterator;

public class CursorIterator implements Iterator<ResultRow> {
    private final Cursor mCursor;
    private final ResultRowFactory mResultRowFactory;

    public CursorIterator(Cursor cursor, ResultRowFactory resultRowFactory) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        if(resultRowFactory == null) throw new IllegalArgumentException("resultRowFactory is null");

        mCursor = cursor;
        mResultRowFactory = resultRowFactory;
    }

    @Override
    public boolean hasNext() {
        return mCursor.moveToNext();
    }

    @Override
    public ResultRow next() {
        return mResultRowFactory.create(mCursor);
    }
    
    @Override
    public void remove() {

    }
}
