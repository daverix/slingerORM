package net.daverix.slingerorm.compiler.mapping.cursor;

import net.daverix.slingerorm.compiler.mapping.Getter;

import java.util.Locale;

public class DeserializeGetter implements Getter {
    private final String serializerName;
    private final Getter value;

    public DeserializeGetter(String serializerName, Getter value) {
        this.serializerName = serializerName;
        this.value = value;
    }

    @Override
    public String get(String cursorVariableName) {
        return String.format(Locale.ENGLISH,
                "%s.deserialize(%s)",
                serializerName,
                value.get(cursorVariableName));
    }
}
