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
package net.daverix.slingerorm.android.serialization;

import net.daverix.slingerorm.annotation.DeserializeType;
import net.daverix.slingerorm.annotation.SerializeType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class TestSerializer {
    @DeserializeType
    public Date deserializeDate(long time) {
        return new Date(time);
    }
    @SerializeType
    public long serializeDate(Date date) {
        return date.getTime();
    }

    @DeserializeType
    public BigDecimal deserializeBigDecimal(double value) {
        return new BigDecimal(value);
    }
    @SerializeType
    public double serializeBigDecimal(BigDecimal bigDecimal) {
        return bigDecimal.doubleValue();
    }

    @DeserializeType
    public UUID deserializeUUID(String id) {
        return UUID.fromString(id);
    }
    @SerializeType
    public String serializeUUID(UUID uuid) {
        return uuid.toString();
    }
}
