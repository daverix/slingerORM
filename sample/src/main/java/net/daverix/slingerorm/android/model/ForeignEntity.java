package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.ColumnName;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class ForeignEntity {
    @PrimaryKey
    private String id;

    @ColumnName("custom_id")
    private CustomFieldEntity custom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CustomFieldEntity getCustom() {
        return custom;
    }

    public void setCustom(CustomFieldEntity custom) {
        this.custom = custom;
    }
}
