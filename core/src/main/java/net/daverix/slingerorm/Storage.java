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
