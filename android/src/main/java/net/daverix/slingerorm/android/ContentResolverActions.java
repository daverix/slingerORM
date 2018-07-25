package net.daverix.slingerorm.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

public class ContentResolverActions implements ContentValuesContainer.Actions {
    private final ContentResolver contentResolver;
    private final TableNameToUriTransformer tableNameToUriTransformer;
    private final UriToIdTransformer uriToIdTransformer;

    public ContentResolverActions(ContentResolver contentResolver,
                                  TableNameToUriTransformer tableNameToUriTransformer,
                                  UriToIdTransformer uriToIdTransformer) {
        this.contentResolver = contentResolver;
        this.tableNameToUriTransformer = tableNameToUriTransformer;
        this.uriToIdTransformer = uriToIdTransformer;
    }

    @Override
    public int update(String tableName, ContentValues values, String where, String[] whereArgs) {
        return contentResolver.update(tableNameToUriTransformer.transform(tableName),
                values,
                where,
                whereArgs);
    }

    @Override
    public long replace(String tableName, ContentValues values) {
        throw new UnsupportedOperationException("Not possible for a content resolver");
    }

    @Override
    public long insert(String tableName, ContentValues values) {
        Uri uri = contentResolver.insert(tableNameToUriTransformer.transform(tableName), values);
        return uriToIdTransformer.transform(uri);
    }

    public interface TableNameToUriTransformer {
        Uri transform(String tableName);
    }

    public interface UriToIdTransformer {
        long transform(Uri uri);
    }
}
