package net.daverix.slingerorm;


import java.io.Closeable;

public interface DataPointer extends Closeable {
    int getCount();
    int getPosition();

    boolean move(int offset);
    boolean moveToPosition(int position);
    boolean moveToFirst();
    boolean moveToLast();
    boolean moveToNext();
    boolean moveToPrevious();

    boolean isFirst();
    boolean isBeforeFirst();
    boolean isAfterLast();

    int getColumnIndex(String key);

    byte[] getBlob(int columnIndex);
    String getString(int columnIndex);
    short getShort(int columnIndex);
    int getInt(int columnIndex);
    long getLong(int columnIndex);
    float getFloat(int columnIndex);
    double getDouble(int columnIndex);
    boolean isNull(int columnIndex);

    void close();
}
