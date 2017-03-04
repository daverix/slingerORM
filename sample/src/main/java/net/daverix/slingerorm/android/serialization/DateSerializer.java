package net.daverix.slingerorm.android.serialization;


import net.daverix.slingerorm.Serializer;

import java.util.Date;

public class DateSerializer implements Serializer<Date,Long> {
    @Override
    public Long serialize(Date value) {
        return value == null ? 0 : value.getTime();
    }

    @Override
    public Date deserialize(Long value) {
        return new Date(value);
    }
}
