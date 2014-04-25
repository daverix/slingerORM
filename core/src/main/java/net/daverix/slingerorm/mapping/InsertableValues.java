package net.daverix.slingerorm.mapping;

public interface InsertableValues {
    public void put(String fieldName, byte[] bytes);
    public void put(String fieldName, String value);

    public void put(String fieldName, double value);
    public void put(String fieldName, float value);

    public void put(String fieldName, int value);
    public void put(String fieldName, boolean value);
    public void put(String fieldName, short value);
    public void put(String fieldName, long value);

    public Object getData();
}
