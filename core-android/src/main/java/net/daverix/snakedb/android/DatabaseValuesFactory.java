package net.daverix.snakedb.android;

import android.content.ContentValues;

import net.daverix.snakedb.mapping.IRetrievableData;
import net.daverix.snakedb.mapping.IRetrievableDataFactory;

public class DatabaseValuesFactory implements IRetrievableDataFactory<ContentValues> {

    public DatabaseValuesFactory() {
    }

    @Override
    public IRetrievableData<ContentValues> create() {
        return new DatabaseValues();
    }
}
