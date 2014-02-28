package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRow;

import javax.inject.Inject;

public class CursorRowResultFactory implements ResultRowFactory {

    @Inject
    public CursorRowResultFactory() {
    }

    @Override
    public ResultRow create(Cursor cursor) {
        return new CursorResultRow(cursor);
    }
}
