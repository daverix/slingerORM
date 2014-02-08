package net.daverix.slingerorm.android.test.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

/**
 * Created by daverix on 2/1/14.
 */
@DatabaseEntity
public class SimpleEntity {
    @PrimaryKey
    public String id;
    public String message;
    public int Length; //should be upper case letter to test both upper and lower case

    public SimpleEntity(String id, String message, int length) {
        this.id = id;
        this.message = message;
        Length = length;
    }

    public SimpleEntity() {
    }
}
