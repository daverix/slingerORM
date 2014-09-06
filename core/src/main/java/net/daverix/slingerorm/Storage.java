package net.daverix.slingerorm;

import net.daverix.slingerorm.exception.StorageException;

import java.util.Collection;

public interface Storage<T> {
    void createTable(DatabaseConnection connection) throws StorageException;

    void insert(DatabaseConnection connection, T item) throws StorageException;
    void update(DatabaseConnection connection, T item) throws StorageException;
    void replace(DatabaseConnection connection, T item) throws StorageException;
    void delete(DatabaseConnection connection, T item) throws StorageException;

    Collection<T> query(DatabaseConnection connection, String selection, String[] selectionArgs, String orderBy) throws StorageException;
    T querySingle(DatabaseConnection connection, String... ids) throws StorageException;
}
