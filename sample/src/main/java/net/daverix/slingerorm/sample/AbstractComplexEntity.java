package net.daverix.slingerorm.sample;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.SetField;

/**
 * Created by daverix on 2/2/14.
 */
@DatabaseEntity
public abstract class AbstractComplexEntity {
    @FieldName("_Id") private long _id;

    @GetField("_id")
    public long getId() {
        return _id;
    }

    @SetField("_id")
    public void setId(long id) {
        _id = id;
    }

}
