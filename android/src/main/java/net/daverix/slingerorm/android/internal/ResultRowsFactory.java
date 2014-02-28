package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRows;

/**
 * Created by daverix on 3/2/14.
 */
public interface ResultRowsFactory {
    ResultRows create(Cursor cursor);
}
