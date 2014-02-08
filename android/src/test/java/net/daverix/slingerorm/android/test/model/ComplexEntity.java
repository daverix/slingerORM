package net.daverix.slingerorm.android.test.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.SetField;

/**
 * Created by daverix on 2/1/14.
 */
@DatabaseEntity(name = "Complex", primaryKey = "_id")
public class ComplexEntity extends AbstractComplexEntity {
    @FieldName("name") private String _name;
    @FieldName("value") private double _value;
    @FieldName("isComplex") private boolean _complex;

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
}
