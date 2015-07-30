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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NativeFieldsEntity)) return false;

        NativeFieldsEntity that = (NativeFieldsEntity) o;

        if (getTypeLong() != that.getTypeLong()) return false;
        if (getTypeInt() != that.getTypeInt()) return false;
        if (getTypeShort() != that.getTypeShort()) return false;
        if (isTypeBoolean() != that.isTypeBoolean()) return false;
        if (Float.compare(that.getTypeFloat(), getTypeFloat()) != 0) return false;
        if (Double.compare(that.getTypeDouble(), getTypeDouble()) != 0) return false;
        return !(getTypeString() != null ? !getTypeString().equals(that.getTypeString()) : that.getTypeString() != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (getTypeLong() ^ (getTypeLong() >>> 32));
        result = 31 * result + getTypeInt();
        result = 31 * result + (int) getTypeShort();
        result = 31 * result + (getTypeString() != null ? getTypeString().hashCode() : 0);
        result = 31 * result + (isTypeBoolean() ? 1 : 0);
        result = 31 * result + (getTypeFloat() != +0.0f ? Float.floatToIntBits(getTypeFloat()) : 0);
        temp = Double.doubleToLongBits(getTypeDouble());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "NativeFieldsEntity{" +
                "typeLong=" + typeLong +
                ", typeInt=" + typeInt +
                ", typeShort=" + typeShort +
                ", typeString='" + typeString + '\'' +
                ", typeBoolean=" + typeBoolean +
                ", typeFloat=" + typeFloat +
                ", typeDouble=" + typeDouble +
                '}';
    }
}
