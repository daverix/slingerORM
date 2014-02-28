package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRows;

import javax.inject.Inject;

/**
 * Created by daverix on 3/2/14.
 */
public class CursorResultsFactory implements ResultRowsFactory {
    private ResultRowFactory mResultRowFactory;

    @Inject
    public CursorResultsFactory(ResultRowFactory resultRowFactory) {
        mResultRowFactory = resultRowFactory;
    }

    @Override
    public ResultRows create(Cursor cursor) {
        return new CursorResultRows(cursor, mResultRowFactory);
    }
}
