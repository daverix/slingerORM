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
package net.daverix.slingerorm.android.entities;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.ColumnName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.SetField;

@DatabaseEntity
public abstract class AbstractComplexEntity {
    @ColumnName("_Id") private long _id;

    @GetField("_id")
    public long getId() {
        return _id;
    }

    @SetField("_id")
    public void setId(long id) {
        _id = id;
    }

}
