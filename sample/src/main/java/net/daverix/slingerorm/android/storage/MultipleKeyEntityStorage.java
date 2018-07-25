package net.daverix.slingerorm.android.storage;

import net.daverix.slingerorm.android.model.MultipleKeyEntity;
import net.daverix.slingerorm.storage.CreateTable;
import net.daverix.slingerorm.storage.DatabaseStorage;

@DatabaseStorage
public interface MultipleKeyEntityStorage {
    @CreateTable(MultipleKeyEntity.class)
    void createTable();
}
