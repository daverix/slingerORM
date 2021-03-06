package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.entity.DatabaseEntity;
import net.daverix.slingerorm.entity.PrimaryKey;

@DatabaseEntity
public class MultipleKeyEntity {
    @PrimaryKey
    private String groupId;
    @PrimaryKey
    private String userId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
