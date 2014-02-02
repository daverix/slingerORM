package net.daverix.snakedb.mapping;

import net.daverix.snakedb.exception.FieldNotFoundException;

/**
 * Created by daverix on 2/1/14.
 */
public interface IInsertableValues {
    public void put(String fieldName, byte[] bytes);
    public void put(String fieldName, String value);

    public void put(String fieldName, double value);
    public void put(String fieldName, float value);

    public void put(String fieldName, int value);
    public void put(String fieldName, short value);
    public void put(String fieldName, long value);
}
