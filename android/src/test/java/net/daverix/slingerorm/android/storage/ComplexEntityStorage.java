package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.model.ComplexEntity;
import net.daverix.slingerorm.android.serialization.TestSerializer;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;

import java.util.List;

@DatabaseStorage(serializer = TestSerializer.class)
public interface ComplexEntityStorage {
    @Insert
    void insert(SQLiteDatabase db, ComplexEntity complexEntity);

    @Update
    void update(SQLiteDatabase db, ComplexEntity complexEntity);

    @Delete
    void delete(SQLiteDatabase db, ComplexEntity complexEntity);

    @Select(where = "_id = ?")
    ComplexEntity getEntity(long id);

    @Select(where = "complex = ?")
    List<ComplexEntity> getComplexEntities(SQLiteDatabase db, boolean complex);
}
