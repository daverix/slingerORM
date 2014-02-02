package net.daverix.snakedb.sample;

import android.provider.BaseColumns;

import net.daverix.snakedb.annotation.DatabaseEntity;
import net.daverix.snakedb.annotation.FieldName;
import net.daverix.snakedb.annotation.GetField;
import net.daverix.snakedb.annotation.PrimaryKey;
import net.daverix.snakedb.annotation.SetField;

/**
 * Created by daverix on 2/2/14.
 */
@DatabaseEntity
public abstract class AbstractComplexEntity {
    @PrimaryKey @FieldName(BaseColumns._ID) private long _id;

    @GetField("_id")
    public long getId() {
        return _id;
    }

    @SetField("_id")
    public void setId(long id) {
        _id = id;
    }

}
