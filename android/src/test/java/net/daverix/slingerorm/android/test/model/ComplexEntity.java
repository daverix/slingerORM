package net.daverix.slingerorm.android.test.model;

import net.daverix.slingerorm.annotation.*;

@DatabaseEntity(name = "Complex", primaryKey = "_id")
public class ComplexEntity extends AbstractComplexEntity {
    @FieldName("name") private String _name;
    @FieldName("value") private double _value;
    @FieldName("isComplex") private boolean _complex;

    @NotDatabaseField
    private String _ignoreThisField;

    @GetField("_name")
    public String getEntityName() {
        return _name;
    }

    @SetField("_name")
    public void setEntityName(String name) {
        _name = name;
    }

    @GetField("_value")
    public double getValue() {
        return _value;
    }

    @SetField("_value")
    public void setValue(double value) {
        _value = value;
    }

    @SetField("_complex")
    public void setComplex(boolean complex) {
        _complex = complex;
    }

    @GetField("_complex")
    public boolean isComplex() {
        return _complex;
    }


    public String getIgnoreThisField() {
        return _ignoreThisField;
    }

    public void setIgnoreThisField(String ignoreThisField) {
        _ignoreThisField = ignoreThisField;
    }
}
