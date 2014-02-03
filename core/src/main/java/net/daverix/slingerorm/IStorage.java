package net.daverix.slingerorm;

import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.exception.InitStorageException;

import java.util.List;

/**
 * Created by daverix on 2/2/14.
 */
public interface IStorage<T> {
    void initStorage() throws InitStorageException;

    void insert(T item) throws FieldNotFoundException;
    void insert(List<T> items) throws FieldNotFoundException;

    void update(T item) throws FieldNotFoundException;
    void update(List<T> items) throws FieldNotFoundException;

    void delete(String item);
    void delete(T item);
    void delete(List<String> ids) throws FieldNotFoundException;

    T get(String id) throws FieldNotFoundException;

    List<T> query(String selection,
                  String[] selectionArgs,
                  String orderBy) throws FieldNotFoundException;
}
