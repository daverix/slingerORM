package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRow;

public interface ResultRowFactory {
    public ResultRow create(Cursor cursor);
}
