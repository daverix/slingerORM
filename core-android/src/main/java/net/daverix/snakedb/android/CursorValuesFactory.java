package net.daverix.snakedb.android;
import android.database.Cursor;

import net.daverix.snakedb.mapping.IFetchableValuesFactory;
import net.daverix.snakedb.mapping.IFetchableValues;

public class CursorValuesFactory implements IFetchableValuesFactory<Cursor> {

    public CursorValuesFactory() {
    }

    @Override
    public IFetchableValues create(Cursor cursor) {
        if(cursor == null) throw new IllegalArgumentException("cursor is null");

        return new CursorValues(cursor);
    }
}
