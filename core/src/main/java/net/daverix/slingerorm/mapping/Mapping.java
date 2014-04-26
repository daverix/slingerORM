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
package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.exception.TypeSerializationException;

public interface Mapping<T> {
    /**
     * Maps data in item to a {@link InsertableValues} object.
     * @param item the item with data
     * @throws net.daverix.slingerorm.exception.FieldNotFoundException when field can't be found in values
     */
    public void mapValues(T item, InsertableValues values) throws FieldNotFoundException, TypeSerializationException;

    /**
     * Creates an object with the data deserialize the database pointer
     * @param row pointer to the database
     * @return an object of the specified template type
     * @throws net.daverix.slingerorm.exception.FieldNotFoundException when field can't be found in values
     */
    public T map(ResultRow row) throws FieldNotFoundException, TypeSerializationException;

    /**
     * Returns a create table statement for the template type
     * @return sql query
     */
    public String getCreateTableSql();

    /**
     * Gets the name of the table
     */
    public String getTableName();

    /**
     * Gets the id of the entity
     */
    public String getId(T item);

    /**
     * Gets the name of the id field
     */
    public String getIdFieldName();
}
