package net.daverix.slingerorm.android;

import android.content.ContentValues;
import android.database.Cursor;

public interface Mapper<T> {
    /**
     * Generates SQL for creating a table for the mapper type
     * @return a generated sql statement
     */
    String createTable();

    /**
     * Gets the name of the table for the given mapper type
     * @return database table name
     */
    String getTableName();

    /**
     * Gets all fields for the given type that should be in the database table
     * @return array of field names
     */
    String[] getFieldNames();

    /**
     * Pulls data from item and puts it into values
     * @param item an item with getters for getting data
     * @param values a standard {@link ContentValues} object to fill with data
     */
    void mapValues(T item, ContentValues values);

    /**
     * Pulls data from cursor and calls setters on item to fill with data
     * @param cursor a standard {@link Cursor} that must have it's pointer set to an element
     * @param item an item with setters for setting data from cursor
     */
    void mapItem(Cursor cursor, T item);
}
