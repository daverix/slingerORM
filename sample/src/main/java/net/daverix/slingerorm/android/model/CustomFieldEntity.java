package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class CustomFieldEntity {
    @FieldName("Id") @PrimaryKey
    private String id;

    @FieldName("Name")
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
