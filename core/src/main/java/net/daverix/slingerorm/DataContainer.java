package net.daverix.slingerorm;


public interface DataContainer {
    DataContainer put(String key, String value);
    DataContainer put(String key, Byte value);
    DataContainer put(String key, Short value);
    DataContainer put(String key, Integer value);
    DataContainer put(String key, Long value);
    DataContainer put(String key, Float value);
    DataContainer put(String key, Double value);
    DataContainer put(String key, Boolean value);
    DataContainer put(String key, byte[] value);
    DataContainer putNull(String key);

    int update(String where, String[] whereArgs);

    long replace();

    long insert();
}
