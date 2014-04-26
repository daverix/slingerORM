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
package net.daverix.slingerorm;

import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.ResultRows;

public interface DatabaseConnection {
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();

    void execSql(String sql);
    void execSql(String sql, String[] args);

    InsertableValues createValues();
    boolean insert(String tableName, InsertableValues values);
    boolean replace(String tableName, InsertableValues values);
    int update(String tableName, InsertableValues values, String selection, String[] selectionArgs);
    int delete(String tableName, String selection, String[] selectionArgs);

    ResultRows query(boolean distinct, String tableName, String[] fields, String selection, String[] selectionArgs, String having, String groupBy, String orderBy);

    void close();
}
