package net.daverix.slingerorm.compiler.mapping.cursor;

import net.daverix.slingerorm.compiler.mapping.Getter;
import net.daverix.slingerorm.compiler.mapping.Setter;

public class FieldSetter implements Setter {
    private final String fieldName;
    private final Getter getter;

    public FieldSetter(String fieldName, Getter getter) {
        this.fieldName = fieldName;
        this.getter = getter;
    }

    @Override
    public String set(String itemVariable, String cursorVariable) {
        return String.format("%s.%s = %s",
                itemVariable, fieldName, getter.get(cursorVariable));
    }
}
