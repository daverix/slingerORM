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

import net.daverix.slingerorm.android.serialization.TestSerializer;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@DatabaseEntity(name = "Medium", serializer = TestSerializer.class)
public class MediumEntity {
    @PrimaryKey
    private String Id;
    private String Name;
    private Date Created;
    private BigDecimal Big;
    private boolean Simple;
    private UUID GroupId;

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Date getCreated() {
        return Created;
    }

    public void setCreated(Date created) {
        Created = created;
    }

    public BigDecimal getBig() {
        return Big;
    }

    public void setBig(BigDecimal big) {
        Big = big;
    }

    public boolean isSimple() {
        return Simple;
    }

    public void setSimple(boolean simple) {
        Simple = simple;
    }

    public UUID getGroupId() {
        return GroupId;
    }

    public void setGroupId(UUID groupId) {
        GroupId = groupId;
    }
}
