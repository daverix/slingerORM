package net.daverix.slingerorm.android.mapping;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.IFetchableValues;

public interface ICursorWrapperFactory {
    public IFetchableValues create(Cursor cursor);
}
