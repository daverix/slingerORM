package net.daverix.slingerorm.android.mapping;

import android.content.ContentValues;

import net.daverix.slingerorm.mapping.IInsertableValues;

public interface IContentValuesWrapper extends IInsertableValues {
    public ContentValues getData();
}
