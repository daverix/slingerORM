package net.daverix.slingerorm.compiler.mapping.cursor;

import net.daverix.slingerorm.compiler.mapping.Getter;
import net.daverix.slingerorm.compiler.mapping.Setter;

public class MethodSetter implements Setter {
    private final String name;
    private final Getter getter;

    public MethodSetter(String name, Getter getter) {
        this.name = name;
        this.getter = getter;
    }

    @Override
    public String set(String itemVariable, String cursorVariable) {
        return String.format("%s.%s(%s)",
                itemVariable, name, getter.get(cursorVariable));
    }
}
