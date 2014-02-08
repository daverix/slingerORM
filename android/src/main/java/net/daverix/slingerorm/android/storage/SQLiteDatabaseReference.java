package net.daverix.slingerorm.android.storage;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by daverix on 2/8/14.
 */
public interface SQLiteDatabaseReference {
    public SQLiteDatabase getReadableDatabase();
    public SQLiteDatabase getWritableDatabase();
}
