package net.daverix.slingerorm.storage;

import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.exception.InitStorageException;

import java.util.List;

/**
 * Created by daverix on 2/2/14.
 */
public interface EntityStorage<T> {
    void initStorage() throws InitStorageException;

    void insert(T item) throws FieldNotFoundException;
    void insert(List<T> items) throws FieldNotFoundException;

    int update(T item) throws FieldNotFoundException;
    int update(List<T> items) throws FieldNotFoundException;

    int delete(String item);
    int delete(T item);
    int delete(List<String> ids) throws FieldNotFoundException;

    T get(String id) throws FieldNotFoundException;

    List<T> query(String selection,
                  String[] selectionArgs,
                  String orderBy) throws FieldNotFoundException;
}
