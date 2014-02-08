package net.daverix.slingerorm.mapping;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by daverix on 2/1/14.
 */
public interface InsertableValues {
    public void put(String fieldName, byte[] bytes);
    public void put(String fieldName, String value);

    public void put(String fieldName, double value);
    public void put(String fieldName, float value);

    public void put(String fieldName, int value);
    public void put(String fieldName, boolean value);
    public void put(String fieldName, short value);
    public void put(String fieldName, long value);
    public void put(String fieldName, Date value);
    public void put(String fieldName, BigDecimal value);
}
