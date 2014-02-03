package net.daverix.snakedb.android.mapping;

import android.database.Cursor;

import net.daverix.snakedb.mapping.IFetchableValues;

public interface ICursorWrapperFactory {
    public IFetchableValues create(Cursor cursor);
}
