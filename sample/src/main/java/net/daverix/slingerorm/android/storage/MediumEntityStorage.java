package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.MediumEntity;
import net.daverix.slingerorm.android.serialization.TestSerializer;
import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;

import java.util.List;

@DatabaseStorage(serializer = TestSerializer.class)
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
