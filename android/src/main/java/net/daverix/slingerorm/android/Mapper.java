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

import java.util.ArrayList;
import java.util.List;

public abstract class Mapper<T> {
    /**
     * Provides SQL for creating a table for the mapper type
     * @return a sql query string
     */
    public abstract String createTable();

    /**
     * Gets the name of the table for the given mapper type
     * @return database table name
     */
    public abstract String getTableName();

    /**
     * Gets all fields for the given type that should be in the database table
     * @return array of field names
     */
    public abstract String[] getFieldNames();

    /**
     * Pulls data from item and puts it into values
     * @param item an item with getters for getting data
     * @return a standard {@link ContentValues} object with mapped data
     */
    public abstract ContentValues mapValues(T item);

    /**
     * Pulls data from cursor and calls setters on item to fill with data. Cursor must be moved to
     * a valid position before calling this method.
     * @param cursor a standard {@link Cursor} that must have it's pointer set to an element
     * @return an instance of {@link T} with mapped data from the cursor
     */
    public abstract T mapItem(Cursor cursor);

    /**
     * Pulls data from cursor and creates instances for each position iterating through the cursor
     * until it's on the last position. Cursor will be moved to the first position automatically.
     * @param cursor a standard {@link Cursor}
     * @return an instance of {@link List<T>} with mapped data from the cursor
     */
    public List<T> mapList(Cursor cursor) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        if(!cursor.moveToFirst()) return new ArrayList<T>();

        List<T> items = new ArrayList<T>();
        do {
            items.add(mapItem(cursor));
        } while(cursor.moveToNext());
        return items;
    }

    /**
     * Provides SQL for updating and deleting an item by the primary key. Use it together with
     * {@link #getItemQueryArguments(T)} to get the correct arguments.
     * @return a sql query string
     */
    public abstract String getItemQuery();

    /**
     * Provides arguments for the sql query provided by {@link #getItemQuery()}
     * @param item the item which should be updated or deleted
     * @return an array of arguments
     */
    public abstract String[] getItemQueryArguments(T item);
}
