package net.daverix.slingerorm.sample;

import android.provider.BaseColumns;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.SetField;

/**
 * Created by daverix on 2/2/14.
 */
@DatabaseEntity
public abstract class AbstractComplexEntity {
    @FieldName(BaseColumns._ID) private long _id;

    @GetField("_id")
    public long getId() {
        return _id;
    }

    @SetField("_id")
    public void setId(long id) {
        _id = id;
    }

}
