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

package net.daverix.slingerorm;

import java.util.List;

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
     * Gets all fields for the given type that should be in the database table
     * @return array of field names
     */
    String[] getFieldNames();

    /**
     * Pulls data from item and puts it into values
     * @param item an item with getters for getting data
     * @param values with mapped data
     */
    void mapValues(T item, DataContainer values);

    /**
     * Pulls data from a pointer and calls setters on item to fill with data.
     * @param dataPointer a pointer that must be set to point at an element
     * @return an instance of {@link T} with mapped data from the pointer
     */
    T mapItem(DataPointer dataPointer);

    /**
     * Pulls data from cursor and creates instances for each position iterating through the cursor
     * until it's on the last position.
     * @param pointer the pointer to get data from, will move to first position when called
     * @return an instance of {@link List<T>} with mapped data from the cursor
     */
    List<T> mapList(DataPointer pointer);

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
