package net.daverix.slingerorm.android.entities;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class PrimaryKeysEntity {
    @PrimaryKey
    public String firstPrimary;
    @PrimaryKey
    public String secondPrimary;
}
