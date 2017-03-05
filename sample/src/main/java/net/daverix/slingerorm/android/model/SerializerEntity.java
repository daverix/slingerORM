package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.serializer.SerializeType;
import net.daverix.slingerorm.entity.DatabaseEntity;
import net.daverix.slingerorm.entity.PrimaryKey;
import net.daverix.slingerorm.entity.SerializeTo;

import java.util.Date;

@DatabaseEntity
public class SerializerEntity {
    @PrimaryKey
    private long id;
    @SerializeTo(SerializeType.LONG)
    private Date created;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
