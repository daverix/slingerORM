package net.daverix.snakedb;

import net.daverix.snakedb.exception.FieldNotFoundException;
import net.daverix.snakedb.exception.InitStorageException;

import java.util.List;

/**
 * Created by daverix on 2/2/14.
 */
public interface IStorage<T> {
    void initStorage() throws InitStorageException;

    void insert(T item) throws FieldNotFoundException;

    void update(T item) throws FieldNotFoundException;

    void delete(T item);

    T get(String id) throws FieldNotFoundException;

    List<T> query(String selection,
                  String[] selectionArgs,
                  String orderBy) throws FieldNotFoundException;
}
