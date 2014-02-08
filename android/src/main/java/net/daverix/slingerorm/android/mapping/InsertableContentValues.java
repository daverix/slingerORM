package net.daverix.slingerorm.android.mapping;

import android.content.ContentValues;

import net.daverix.slingerorm.mapping.InsertableValues;

public interface InsertableContentValues extends InsertableValues {
    public ContentValues getData();
}
