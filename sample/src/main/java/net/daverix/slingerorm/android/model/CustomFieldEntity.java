package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.ColumnName;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class CustomFieldEntity {
    @ColumnName("Id") @PrimaryKey
    private String id;

    @ColumnName("Name")
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
