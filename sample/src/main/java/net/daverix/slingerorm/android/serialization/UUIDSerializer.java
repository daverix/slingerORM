package net.daverix.slingerorm.android.serialization;


import net.daverix.slingerorm.Serializer;

import java.util.UUID;

public class UUIDSerializer implements Serializer<UUID,String> {
    @Override
    public String serialize(UUID value) {
        return value == null ? null : value.toString();
    }

    @Override
    public UUID deserialize(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
