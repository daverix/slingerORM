/*
 * Copyright 2015 David Laurell
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

package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

public interface Mapper<T> {
    /**
     * Generates SQL for creating a table for the mapper type
     * @return a generated sql statement
     */
    String createTable();

    /**
     * Gets the name of the table for the given mapper type
     * @return database table name
     */
    String getTableName();

    /**
     * Gets all fields for the given type that should be in the database table
     * @return array of field names
     */
    String[] getFieldNames();

    /**
     * Pulls data from item and puts it into values
     * @param item an item with getters for getting data
     * @return a standard {@link ContentValues} object with mapped data
     */
    ContentValues mapValues(T item);

    /**
     * Pulls data from cursor and calls setters on item to fill with data. Cursor must be moved to
     * a valid position before calling this method.
     * @param cursor a standard {@link Cursor} that must have it's pointer set to an element
     * @return an instance of {@link T} with mapped data from the cursor
     */
    T mapItem(Cursor cursor);

    /**
     * Pulls data from cursor and creates instances for each position iterating through the cursor
     * until it's on the last position. Cursor will be moved to the first position automatically.
     * @param cursor a standard {@link Cursor}
     * @return an instance of {@link List<T>} with mapped data from the cursor
     */
    List<T> mapList(Cursor cursor);
}
