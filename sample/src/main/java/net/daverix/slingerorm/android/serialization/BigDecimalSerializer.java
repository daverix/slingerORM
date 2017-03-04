package net.daverix.slingerorm.android.serialization;


import net.daverix.slingerorm.Serializer;

import java.math.BigDecimal;

public class BigDecimalSerializer implements Serializer<BigDecimal,Double> {
    @Override
    public Double serialize(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    @Override
    public BigDecimal deserialize(Double value) {
        return new BigDecimal(value);
    }
}
