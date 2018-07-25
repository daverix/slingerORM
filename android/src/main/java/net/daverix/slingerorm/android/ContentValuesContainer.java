package net.daverix.slingerorm.android;

import android.content.ContentValues;

import net.daverix.slingerorm.DataContainer;

public class ContentValuesContainer implements DataContainer {
    private final ContentValues values = new ContentValues();
    private final String table;
    private final Actions actions;

    public ContentValuesContainer(String table, Actions actions) {
        this.actions = actions;
        this.table = table;
    }

    @Override
    public DataContainer put(String key, String value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Byte value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Short value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Integer value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Long value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Float value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Double value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, Boolean value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer put(String key, byte[] value) {
        values.put(key, value);
        return this;
    }

    @Override
    public DataContainer putNull(String key) {
        values.putNull(key);
        return this;
    }

    @Override
    public int update(String where, String[] whereArgs) {
        return actions.update(table, values, where, whereArgs);
    }

    @Override
    public long replace() {
        return actions.replace(table, values);
    }

    @Override
    public long insert() {
        return actions.insert(table, values);
    }

    public interface Actions {
        int update(String tableName, ContentValues values, String where, String[] whereArgs);
        long replace(String tableName, ContentValues values);
        long insert(String tableName, ContentValues values);
    }
}
