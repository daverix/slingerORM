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

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.MediumEntity;
import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;

import java.util.List;

@DatabaseStorage
public interface MediumEntityStorage {
    @CreateTable(MediumEntity.class)
    void createTable(SQLiteDatabase db);

    @Insert
    void insert(SQLiteDatabase db, MediumEntity entity);

    @Update
    void update(SQLiteDatabase db, MediumEntity entity);

    @Replace
    void replace(SQLiteDatabase db, MediumEntity entity);

    @Delete
    void delete(SQLiteDatabase db, MediumEntity entity);

    @Select(where = "Id = ?")
    MediumEntity getEntity(SQLiteDatabase db, long id);

    @Select(where = "Simple = ?")
    List<MediumEntity> getComplexEntities(SQLiteDatabase db, boolean simple);
}
