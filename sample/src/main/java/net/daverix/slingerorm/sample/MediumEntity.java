package net.daverix.slingerorm.sample;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

/**
 * Created by daverix on 2/1/14.
 */
@DatabaseEntity("Medium")
public class MediumEntity {
    @PrimaryKey
    private String Id;
    private String Name;
    private long Size;

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public long getSize() {
        return Size;
    }

    public void setSize(long size) {
        Size = size;
    }
}
