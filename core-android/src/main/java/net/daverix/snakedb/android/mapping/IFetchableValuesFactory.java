package net.daverix.snakedb.android.mapping;

import android.database.Cursor;

import net.daverix.snakedb.mapping.IFetchableValues;

public interface IFetchableValuesFactory {
    public IFetchableValues create(Cursor cursor);
}
