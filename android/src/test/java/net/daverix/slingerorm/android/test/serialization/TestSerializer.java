package net.daverix.slingerorm.android.test.serialization;

import net.daverix.slingerorm.annotation.DeserializeType;
import net.daverix.slingerorm.annotation.SerializeType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class TestSerializer {
    @DeserializeType
    public Date deserializeDate(long time) {
        return new Date(time);
    }
    @SerializeType
    public long serializeDate(Date date) {
        return date.getTime();
    }

    @DeserializeType
    public BigDecimal deserializeBigDecimal(double value) {
        return new BigDecimal(value);
    }
    @SerializeType
    public double serializeBigDecimal(BigDecimal bigDecimal) {
        return bigDecimal.doubleValue();
    }

    @DeserializeType
    public UUID deserializeUUID(String id) {
        return UUID.fromString(id);
    }
    @SerializeType
    public String serializeUUID(UUID uuid) {
        return uuid.toString();
    }
}
