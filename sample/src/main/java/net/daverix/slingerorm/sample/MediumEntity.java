package net.daverix.slingerorm.sample;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by daverix on 2/1/14.
 */
@DatabaseEntity(name = "Medium")
public class MediumEntity {
    @PrimaryKey
    private String Id;
    private String Name;
    private Date Created;
    private BigDecimal Big;
    private boolean Simple;

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

    public Date getCreated() {
        return Created;
    }

    public void setCreated(Date created) {
        Created = created;
    }

    public BigDecimal getBig() {
        return Big;
    }

    public void setBig(BigDecimal big) {
        Big = big;
    }

    public boolean isSimple() {
        return Simple;
    }

    public void setSimple(boolean simple) {
        Simple = simple;
    }
}
