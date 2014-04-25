package net.daverix.slingerorm;

import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.ResultRows;

public interface DatabaseConnection {
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();

    void execSql(String sql);
    void execSql(String sql, String[] args);

    InsertableValues createValues();
    boolean insert(String tableName, InsertableValues values);
    boolean replace(String tableName, InsertableValues values);
    int update(String tableName, InsertableValues values, String selection, String[] selectionArgs);
    int delete(String tableName, String selection, String[] selectionArgs);

    ResultRows query(boolean distinct, String tableName, String[] fields, String selection, String[] selectionArgs, String having, String groupBy, String orderBy);

    void close();
}
