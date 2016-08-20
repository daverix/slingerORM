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

public interface Mapper<T> {
    /**
     * Provides SQL for creating a table for the mapper type
     * @return a sql query string
     */
    String createTable();

    /**
     * Gets the name of the table for the given mapper type
     * @return database table name
     */
    String getTableName();

    /**
     * Gets all columns for the given type in the database table
     * @return array of column names
     */
    String[] getColumnNames();

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
     * Provides SQL for updating and deleting an item by the primary key. Use it together with
     * {@link #getItemQueryArguments(T)} to get the correct arguments.
     * @return a sql query string
     */
    String getItemQuery();

    /**
     * Provides arguments for the sql query provided by {@link #getItemQuery()}
     * @param item the item which should be updated or deleted
     * @return an array of arguments
     */
    String[] getItemQueryArguments(T item);
}
