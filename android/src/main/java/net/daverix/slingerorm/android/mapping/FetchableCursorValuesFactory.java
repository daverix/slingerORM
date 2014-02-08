package net.daverix.slingerorm.android.mapping;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.FetchableValues;

public interface FetchableCursorValuesFactory {
    public FetchableValues create(Cursor cursor);
}
