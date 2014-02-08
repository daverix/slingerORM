package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.exception.FieldNotFoundException;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by daverix on 2/1/14.
 */
public interface FetchableValues {
    public byte[] getBlob(String fieldName) throws FieldNotFoundException;
    public String getString(String fieldName) throws FieldNotFoundException;

    public double getDouble(String fieldName) throws FieldNotFoundException;
    public float getFloat(String fieldName) throws FieldNotFoundException;

    public int getInt(String fieldName) throws FieldNotFoundException;
    public short getShort(String fieldName) throws FieldNotFoundException;
    public long getLong(String fieldName) throws FieldNotFoundException;
    public boolean getBoolean(String fieldName) throws FieldNotFoundException;

    public BigDecimal getBigDecimal(String fieldName) throws FieldNotFoundException;
    public Date getDate(String fieldName) throws FieldNotFoundException;
}
