package net.daverix.slingerorm.compiler.mapping.values;

import net.daverix.slingerorm.compiler.mapping.Getter;

import java.util.Locale;

public class SerializeGetter implements Getter {
    private final String name;
    private final Getter value;

    public SerializeGetter(String name, Getter value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String get(String variable) {
        return String.format(Locale.ENGLISH,
                "%s.serialize(%s)",
                name, value.get(variable));
    }
}
