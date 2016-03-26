package net.daverix.slingerorm.serialization;

import java.util.UUID;

public class UUIDSerializer implements TypeSerializer<UUID, String> {
    @Override
    public String serialize(UUID in) {
        return in == null ? null : in.toString();
    }

    @Override
    public UUID deserialize(String in) {
        return in == null ? null : UUID.fromString(in);
    }
}
