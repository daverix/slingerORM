package net.daverix.slingerorm.compiler;


import net.daverix.slingerorm.compiler.mapping.Getter;

public class ColumnModel {
    private final String name;
    private final ColumnDataType columnDataType;
    private final boolean primaryKey;
    private final Getter getter;

    public ColumnModel(String name, ColumnDataType columnDataType, boolean primaryKey, Getter getter) {
        this.name = name;
        this.columnDataType = columnDataType;
        this.primaryKey = primaryKey;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    public ColumnDataType getDataType() {
        return columnDataType;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public Getter getGetter() {
        return getter;
    }
}
