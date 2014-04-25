package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRows;

public interface ResultRowsFactory {
    ResultRows create(Cursor cursor);
}
