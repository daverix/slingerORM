package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.android.serialization.TestSerializer;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

import java.util.Date;

@DatabaseEntity(serializer = TestSerializer.class)
public class SerializerEntity {
    @PrimaryKey
    private long id;
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
