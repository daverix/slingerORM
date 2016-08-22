package net.daverix.slingerorm.compiler.mapping.values;


import net.daverix.slingerorm.compiler.mapping.Getter;

public class FieldValueGetter implements Getter {
    private final String fieldName;

    public FieldValueGetter(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String get(String itemVariable) {
        return String.format("%s.%s", itemVariable, fieldName);
    }
}
