package net.daverix.snakedb.android.mapping;

import android.content.ContentValues;

import net.daverix.snakedb.mapping.IInsertableValues;

public interface IContentValuesWrapper extends IInsertableValues {
    public ContentValues getData();
}
