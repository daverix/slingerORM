package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.SetField;

@DatabaseEntity(primaryKeyFields = "mId")
public class GetterSetterEntity {
    private String mId;
    private int mNumber;

    @SetField("mId")
    public void setId(String id) {
        mId = id;
    }

    @GetField("mId")
    public String getId() {
        return mId;
    }

    @SetField("mNumber")
    public void setNumber(int number) {
        mNumber = number;
    }

    @GetField("mNumber")
    public int getNumber() {
        return mNumber;
    }
}
