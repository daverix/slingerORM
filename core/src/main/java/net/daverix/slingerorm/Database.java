package net.daverix.slingerorm;

public interface Database {
    void execSQL(String sql);

    DataPointer query(boolean distinct, String tableName, String[] columns, String where,
                      String[] whereArgs, String groupBy, String having, String orderBy, String limit);

    int delete(String tableName, String where, String[] whereArgs);

    DataContainer edit(String tableName);
}
