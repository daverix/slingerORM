package net.daverix.slingerorm.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlingerStorage implements Storage {
    private final Map<Class<?>, Mapper<?>> mappers = new HashMap<Class<?>, Mapper<?>>();
    private final AbstractDatabaseProxy dbp;

    public SlingerStorage(AbstractDatabaseProxy dbp) {
        this.dbp = dbp;
    }

    public SlingerStorage(final SQLiteDatabase db) {
        this.dbp = new AbstractSQLiteDatabaseProxy() {
            @Override
            public SQLiteDatabase createDatabase() {
                return db;
            }
        };
    }

    public <T> void registerMapper(Class<T> clazz, Mapper<T> mapper) {
        mappers.put(clazz, mapper);
    }

    @SuppressWarnings("unchecked")
    private <T> Mapper<T> getMapper(Class<?> clazz) {
        Mapper<T> mapper = (Mapper<T>) mappers.get(clazz);
        if(mapper == null)
            throw new IllegalStateException(String.format("Mapper for type %s not found, is the type annotated with @DatabaseEntity?", clazz.getCanonicalName()));

        return mapper;
    }

    @Override
    public <T> void createTable(Class<T> clazz) {
        Mapper<T> mapper = getMapper(clazz);
        dbp.execSql(mapper.createTable());
    }

    @Override
    public <T> SelectBuilder<T> select(Class<T> clazz) {
        Mapper<T> mapper = getMapper(clazz);
        return new DatabaseProxySelectBuilder<T>(dbp, mapper);
    }

    @Override @SuppressWarnings("unchecked")
    public <T> void insert(T item) {
        Mapper<T> mapper = getMapper(item.getClass());
        dbp.insert(mapper.getTableName(), mapper.mapValues(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> void replace(T item) {
        Mapper<T> mapper = getMapper(item.getClass());
        dbp.replace(mapper.getTableName(), mapper.mapValues(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> int update(T item) {
        Mapper<T> mapper = getMapper(item.getClass());
        return dbp.update(mapper.getTableName(), mapper.mapValues(item), mapper.getItemQuery(), mapper.getItemQueryArguments(item));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> int delete(T item) {
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
                cursor = dbp.query(distinct, mapper.getTableName(), mapper.getFieldNames(), where, whereArgs, groupBy, having, orderBy, limit);
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
                cursor = dbp.query(distinct, mapper.getTableName(), mapper.getFieldNames(), where, whereArgs, groupBy, having, orderBy, limit);
                if(cursor == null) return new ArrayList<T>();

                return mapper.mapList(cursor);
            } finally {
                if(cursor != null) cursor.close();
            }
        }
    }
}
