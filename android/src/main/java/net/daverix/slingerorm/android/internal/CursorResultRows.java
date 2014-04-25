package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRow;
import net.daverix.slingerorm.mapping.ResultRows;

import java.util.Iterator;

public class CursorResultRows implements ResultRows {
    private final Cursor mCursor;
    private final ResultRowFactory mResultRowFactory;

    public CursorResultRows(Cursor cursor, ResultRowFactory resultRowFactory) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        if(resultRowFactory == null) throw new IllegalArgumentException("resultRowFactory is null");
        mCursor = cursor;
        mResultRowFactory = resultRowFactory;
    }

    @Override
    public void close() {
        mCursor.close();
    }

    @Override
    public Iterator<ResultRow> iterator() {
        return new CursorIterator(mCursor, mResultRowFactory);
    }
}
