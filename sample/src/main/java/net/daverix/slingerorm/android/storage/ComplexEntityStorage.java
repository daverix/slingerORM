package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.ComplexEntity;
import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;

import java.util.List;

@DatabaseStorage
public interface ComplexEntityStorage {
    @CreateTable(ComplexEntity.class)
    void createTable(SQLiteDatabase db);

    @Insert
    void insert(SQLiteDatabase db, ComplexEntity complexEntity);

    @Update
    void update(SQLiteDatabase db, ComplexEntity complexEntity);

    @Replace
    void replace(SQLiteDatabase db, ComplexEntity complexEntity);

    @Delete
    void delete(SQLiteDatabase db, ComplexEntity complexEntity);

    @Select(where = "_id = ?")
    ComplexEntity getEntity(SQLiteDatabase db, long id);

    @Select(where = "isComplex = ?")
    List<ComplexEntity> getComplexEntities(SQLiteDatabase db, boolean complex);
}
