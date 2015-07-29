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

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class NativeFieldsEntity {
    @PrimaryKey
    private long typeLong;
    private int typeInt;
    private short typeShort;
    private String typeString;
    private boolean typeBoolean;
    private float typeFloat;
    private double typeDouble;

    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public short getTypeShort() {
        return typeShort;
    }

    public void setTypeShort(short typeShort) {
        this.typeShort = typeShort;
    }

    public int getTypeInt() {
        return typeInt;
    }

    public void setTypeInt(int typeInt) {
        this.typeInt = typeInt;
    }

    public long getTypeLong() {
        return typeLong;
    }

    public void setTypeLong(long typeLong) {
        this.typeLong = typeLong;
    }

    public boolean isTypeBoolean() {
        return typeBoolean;
    }

    public void setTypeBoolean(boolean typeBoolean) {
        this.typeBoolean = typeBoolean;
    }

    public float getTypeFloat() {
        return typeFloat;
    }

    public void setTypeFloat(float typeFloat) {
        this.typeFloat = typeFloat;
    }

    public double getTypeDouble() {
        return typeDouble;
    }

    public void setTypeDouble(double typeDouble) {
        this.typeDouble = typeDouble;
    }
}
