/*
 * Copyright 2015 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.*;

@DatabaseEntity(name = "Complex", primaryKeyFields = "_id")
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

    @Override
    public String toString() {
        return "ComplexEntity{" +
                "_name='" + _name + '\'' +
                ", _value=" + _value +
                ", _complex=" + _complex +
                ", _ignoreThisField='" + _ignoreThisField + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplexEntity)) return false;

        ComplexEntity that = (ComplexEntity) o;

        if (Double.compare(that._value, _value) != 0) return false;
        if (_complex != that._complex) return false;
        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        return !(_ignoreThisField != null ? !_ignoreThisField.equals(that._ignoreThisField) : that._ignoreThisField != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = _name != null ? _name.hashCode() : 0;
        temp = Double.doubleToLongBits(_value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (_complex ? 1 : 0);
        result = 31 * result + (_ignoreThisField != null ? _ignoreThisField.hashCode() : 0);
        return result;
    }
}
