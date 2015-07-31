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

public interface Storage {
    <T> void createTable(Class<T> clazz);

    <T> SelectBuilder<T> select(Class<T> clazz);

    <T> void insert(T item);
    <T> void replace(T item);
    <T> int update(T item);
    <T> int delete(T item);

    void beginTransaction();
    void endTransaction();
    void setTransactionSuccessful();

    interface SelectBuilder<T> {
        SelectBuilder<T> distinct(boolean distinct);
        SelectBuilder<T> where(String where, String... args);
        SelectBuilder<T> having(String having);
        SelectBuilder<T> groupBy(String groupBu);
        SelectBuilder<T> orderBy(String orderBy);
        SelectBuilder<T> limit(String limit);
        T first();
        List<T> toList();
    }
}
