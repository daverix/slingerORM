package net.daverix.slingerorm.android.storage;

import net.daverix.slingerorm.android.model.GetterSetterEntity;
import net.daverix.slingerorm.storage.CreateTable;
import net.daverix.slingerorm.storage.Insert;
import net.daverix.slingerorm.storage.Select;

public interface GetterSetterEntityStorage {
    @CreateTable(GetterSetterEntity.class)
    void createTable();

    @Insert
    void insert(GetterSetterEntity entity);

    @Select
    GetterSetterEntity getEntity(String id);
}
