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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.annotation.DatabaseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlingerStorage implements Storage {
    private final Map<Class<?>, Mapper<?>> mappers = new HashMap<Class<?>, Mapper<?>>();
    private final AbstractDatabaseProxy dbp;

    public SlingerStorage(AbstractDatabaseProxy dbp) {
        if(dbp == null) throw new IllegalArgumentException("dbp is null");

        this.dbp = dbp;
    }

    public SlingerStorage(final SQLiteDatabase db) {
        if(db == null) throw new IllegalArgumentException("db is null");

        this.dbp = new SQLiteDatabaseProxy(db);
    }

    @Override
    public <T> void registerMapper(Class<T> clazz, Mapper<T> mapper) {
        if(clazz == null) throw new IllegalArgumentException("clazz is null");
        if(mapper == null) throw new IllegalArgumentException("mapper is null");

        if(!clazz.isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(clazz + " must be annotated with @DatabaseEntity");

        mappers.put(clazz, mapper);
    }

    @SuppressWarnings("unchecked")
    private <T> Mapper<T> getMapper(Class<?> clazz) {
        Mapper<T> mapper = (Mapper<T>) mappers.get(clazz);
        if(mapper == null)
            throw new IllegalStateException(String.format("Mapper for type %s not registered", clazz.getCanonicalName()));

        return mapper;
    }

    @Override
    public <T> void createTable(Class<T> clazz) {
        if(clazz == null)
            throw new IllegalArgumentException("clazz is null");

        if(!clazz.isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(clazz + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(clazz);
        dbp.execSql(mapper.createTable());
    }

    @Override
    public <T> SelectBuilder<T> select(Class<T> clazz) {
        if(clazz == null)
            throw new IllegalArgumentException("clazz is null");

        if(!clazz.isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(clazz + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(clazz);
        return new DatabaseProxySelectBuilder<T>(dbp, mapper);
    }

    @Override @SuppressWarnings("unchecked")
    public <T> void insert(T item) {
        if(item == null)
            throw new IllegalArgumentException("item is null");

        if(!item.getClass().isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(item.getClass() + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(item.getClass());
        dbp.insert(mapper.getTableName(), mapper.mapValues(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> void replace(T item) {
        if(item == null)
            throw new IllegalArgumentException("item is null");

        if(!item.getClass().isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(item.getClass() + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(item.getClass());
        dbp.replace(mapper.getTableName(), mapper.mapValues(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> int update(T item) {
        if(item == null)
            throw new IllegalArgumentException("item is null");

        if(!item.getClass().isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(item.getClass() + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(item.getClass());
        return dbp.update(mapper.getTableName(), mapper.mapValues(item), mapper.getItemQuery(), mapper.getItemQueryArguments(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> int delete(T item) {
        if(item == null)
            throw new IllegalArgumentException("item is null");

        if(!item.getClass().isAnnotationPresent(DatabaseEntity.class))
            throw new IllegalArgumentException(item.getClass() + " must be annotated with @DatabaseEntity");

        Mapper<T> mapper = getMapper(item.getClass());
        return dbp.delete(mapper.getTableName(), mapper.getItemQuery(), mapper.getItemQueryArguments(item));
    }

    @Override
    public void beginTransaction() {
        dbp.beginTransaction();
    }

    @Override
    public void endTransaction() {
        dbp.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        dbp.setTransactionSuccessful();
    }

    private class DatabaseProxySelectBuilder<T> implements Storage.SelectBuilder<T> {
        private final AbstractDatabaseProxy dbp;
        private final Mapper<T> mapper;
        private String where;
        private String[] whereArgs;
        private String having;
        private String groupBy;
        private String orderBy;
        private String limit;
        private boolean distinct;

        public DatabaseProxySelectBuilder(AbstractDatabaseProxy dbp, Mapper<T> mapper) {
            this.dbp = dbp;
            this.mapper = mapper;
        }

        @Override
        public SelectBuilder<T> distinct(boolean distinct) {
            this.distinct = distinct;
            return this;
        }

        @Override
        public Storage.SelectBuilder<T> where(String where, String... args) {
            this.where = where;
            this.whereArgs = args;
            return this;
        }

        @Override
        public Storage.SelectBuilder<T> having(String having) {
            this.having = having;
            return this;
        }

        @Override
        public Storage.SelectBuilder<T> groupBy(String groupBy) {
            this.groupBy = groupBy;
            return this;
        }

        @Override
        public Storage.SelectBuilder<T> orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        @Override
        public Storage.SelectBuilder<T> limit(String limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public T first() {
            Cursor cursor = null;
            try {
                cursor = query();
                if(cursor == null || !cursor.moveToFirst()) return null;

                return mapper.mapItem(cursor);
            } finally {
                if(cursor != null) cursor.close();
            }
        }

        @Override
        public List<T> toList() {
            Cursor cursor = null;
            try {
                cursor = query();
                if(cursor == null) return new ArrayList<T>();

                return mapList(mapper, cursor);
            } finally {
                if(cursor != null) cursor.close();
            }
        }

        private Cursor query() {
            return dbp.query(distinct,
                    mapper.getTableName(),
                    mapper.getColumnNames(),
                    where,
                    whereArgs,
                    groupBy,
                    having,
                    orderBy,
                    limit);
        }
    }

    /**
     * Pulls data from cursor and creates instances for each position iterating through the cursor
     * until it's on the last position. Cursor will be moved to the first position automatically.
     * @param mapper the mapper to map single items from
     * @param cursor a standard {@link Cursor}
     * @return an instance of {@link List<T>} with mapped data from the cursor
     */
    public static <T> List<T> mapList(Mapper<T> mapper, Cursor cursor) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        if(!cursor.moveToFirst()) return new ArrayList<T>();

        List<T> items = new ArrayList<T>();
        do {
            items.add(mapper.mapItem(cursor));
        } while(cursor.moveToNext());
        return items;
    }
}
