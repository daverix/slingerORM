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
