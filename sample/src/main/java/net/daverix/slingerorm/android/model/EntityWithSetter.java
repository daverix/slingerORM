package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.SetField;

public class EntityWithSetter {
    @FieldName("hello")
    private String _hello;

    @SetField("_hello")
    public void setHej(String hello) {
        _hello = hello;
    }
}
