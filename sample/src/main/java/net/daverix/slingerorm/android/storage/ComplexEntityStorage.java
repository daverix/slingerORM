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

package net.daverix.slingerorm.android.storage;

import net.daverix.slingerorm.android.model.ComplexEntity;
import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Limit;
import net.daverix.slingerorm.annotation.OrderBy;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Update;
import net.daverix.slingerorm.annotation.Where;

import java.util.List;

@DatabaseStorage
public interface ComplexEntityStorage {
    @CreateTable(ComplexEntity.class)
    void createTable();

    @Insert
    void insert(ComplexEntity complexEntity);

    @Update
    void update(ComplexEntity complexEntity);

    @Replace
    void replace(ComplexEntity complexEntity);

    @Delete
    void delete(ComplexEntity complexEntity);

    @Delete(ComplexEntity.class) @Where("_id = ?")
    int delete(long id);

    @Select @Where("_id = ?")
    ComplexEntity getEntity(long id);

    @Select
    List<ComplexEntity> getAllEntities();

    @Select @Where("isComplex = ?") @OrderBy("value DESC") @Limit("2")
    List<ComplexEntity> getComplexEntities(boolean complex);
}
