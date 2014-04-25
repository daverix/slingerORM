package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRows;

import javax.inject.Inject;

public class CursorResultsFactory implements ResultRowsFactory {
    private ResultRowFactory mResultRowFactory;

    @Inject
    public CursorResultsFactory(ResultRowFactory resultRowFactory) {
        if(resultRowFactory == null) throw new IllegalArgumentException("resultRowFactory is null");
        mResultRowFactory = resultRowFactory;
    }

    public CursorResultsFactory() {
        mResultRowFactory = new CursorRowResultFactory();
    }

    @Override
    public ResultRows create(Cursor cursor) {
        return new CursorResultRows(cursor, mResultRowFactory);
    }
}
