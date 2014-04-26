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

import net.daverix.slingerorm.mapping.ResultRows;

import javax.inject.Inject;

public class CursorResultsFactory implements ResultRowsFactory {
    private ResultRowFactory mResultRowFactory;

    @Inject
    public CursorResultsFactory(ResultRowFactory resultRowFactory) {
        if(resultRowFactory == null) throw new IllegalArgumentException("resultRowFactory is null");
        mResultRowFactory = resultRowFactory;
    }

    public CursorResultsFactory() {
        mResultRowFactory = new CursorRowResultFactory();
    }

    @Override
    public ResultRows create(Cursor cursor) {
        return new CursorResultRows(cursor, mResultRowFactory);
    }
}
