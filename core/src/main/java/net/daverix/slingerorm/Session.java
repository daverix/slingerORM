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

import net.daverix.slingerorm.exception.SessionException;

import java.util.Collection;

public interface Session {
    <T> void initTable(Class<T> entityClass) throws SessionException;

    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();

    <T> void insert(T item) throws SessionException;
    <T> void update(T item) throws SessionException;
    <T> void replace(T item) throws SessionException;
    <T> void delete(T item) throws SessionException;

    <T> Collection<T> query(Class<T> entityClass, String selection, String[] selectionArgs, String orderBy) throws SessionException;
    <T> T querySingle(Class<T> entityClass, String id) throws SessionException;

    void close();
}
