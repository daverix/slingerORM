package net.daverix.slingerorm.android.internal;

import android.database.Cursor;

import net.daverix.slingerorm.mapping.ResultRow;

import java.util.Iterator;

public class CursorIterator implements Iterator<ResultRow> {
    private final Cursor mCursor;
    private final ResultRowFactory mResultRowFactory;

    public CursorIterator(Cursor cursor, ResultRowFactory resultRowFactory) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");
        if(resultRowFactory == null) throw new IllegalArgumentException("resultRowFactory is null");

        mCursor = cursor;
        mResultRowFactory = resultRowFactory;
    }

    @Override
    public boolean hasNext() {
        return mCursor.moveToNext();
    }

    @Override
    public ResultRow next() {
        return mResultRowFactory.create(mCursor);
    }
    
    @Override
    public void remove() {

    }
}
