/*
 * Copyright 2014 David Laurell
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
