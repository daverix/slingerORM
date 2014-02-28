package net.daverix.slingerorm;

import net.daverix.slingerorm.exception.FetchMappingException;
import net.daverix.slingerorm.exception.SessionException;
import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.Mapping;
import net.daverix.slingerorm.mapping.ResultRow;
import net.daverix.slingerorm.mapping.ResultRows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SlingerSession implements Session {
    private final DatabaseConnection mDatabaseConnection;
    private final MappingFetcher mMappingFetcher;

    public SlingerSession(DatabaseConnection databaseConnection, MappingFetcher mappingFetcher) {
        if(databaseConnection == null) throw new IllegalArgumentException("databaseConnection is null");
        if(mappingFetcher == null) throw new IllegalArgumentException("mappingFetcher is null");
        mDatabaseConnection = databaseConnection;
        mMappingFetcher = mappingFetcher;
    }


    @Override
    public <T> void initTable(Class<T> entityClass) throws SessionException {
        try {
            Mapping<T> mapping = mMappingFetcher.fetchMapping(entityClass);
            mDatabaseConnection.execSql(mapping.getCreateTableSql());
        } catch (FetchMappingException e) {
            throw new SessionException(e);
        }
    }

    @Override
    public void beginTransaction() {
        mDatabaseConnection.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        mDatabaseConnection.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        mDatabaseConnection.endTransaction();
    }

    @Override
    public <T> void insert(T item) throws SessionException {
        try {
            Mapping<T> mapping = getMapping(item);
            InsertableValues insertableValues = mDatabaseConnection.createValues();
            mapping.mapValues(item, insertableValues);
            mDatabaseConnection.insert(mapping.getTableName(), insertableValues);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public <T> void replace(T item) throws SessionException {
        try {
            Mapping<T> mapping = getMapping(item);
            InsertableValues insertableValues = mDatabaseConnection.createValues();
            mapping.mapValues(item, insertableValues);
            mDatabaseConnection.replace(mapping.getTableName(), insertableValues);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public <T> void update(T item) throws SessionException {
        try {
            Mapping<T> mapping = getMapping(item);
            InsertableValues values =  mDatabaseConnection.createValues();
            mapping.mapValues(item, values);
            mDatabaseConnection.update(mapping.getTableName(), values, mapping.getIdFieldName() + "=?", new String[]{mapping.getId(item)});
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public <T> void delete(T item) throws SessionException {
        try {
            Mapping<T> mapping = getMapping(item);
            mDatabaseConnection.delete(mapping.getTableName(), mapping.getIdFieldName() + "=?", new String[]{mapping.getId(item)});
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public <T> Collection<T> query(Class<T> entityClass, String selection, String[] selectionArgs, String orderBy) throws SessionException {
        List<T> items = new ArrayList<T>();
        try {
            Mapping<T> mapping = mMappingFetcher.fetchMapping(entityClass);
            ResultRows result = null;
            try {
                result = mDatabaseConnection.query(false, mapping.getTableName(), null, selection, selectionArgs, null, null, orderBy);
                for(ResultRow values : result) {
                    items.add(mapping.map(values));
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
            return items;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public <T> T querySingle(Class<T> entityClass, String id) throws SessionException {
        try {
            Mapping<T> mapping = mMappingFetcher.fetchMapping(entityClass);
            ResultRows result = null;
            try {
                result = mDatabaseConnection.query(false, mapping.getTableName(), null,
                        mapping.getIdFieldName() + "=?", new String[]{id}, null, null, null);

                if(result.iterator().hasNext()) {
                    return mapping.map(result.iterator().next());
                }
                else {
                    return null;
                }
            } finally {
                if(result != null) {
                    result.close();
                }
            }
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    @Override
    public void close() {
        mDatabaseConnection.close();
    }

    protected <T> Mapping<T> getMapping(T item) throws FetchMappingException {
        @SuppressWarnings("unchecked") Class<T> itemClass = (Class<T>)item.getClass();
        return mMappingFetcher.fetchMapping(itemClass);
    }
}
