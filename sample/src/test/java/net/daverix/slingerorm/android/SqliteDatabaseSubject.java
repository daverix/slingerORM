package net.daverix.slingerorm.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;

public class SqliteDatabaseSubject extends Subject<SqliteDatabaseSubject, SQLiteDatabase> {
    private SqliteDatabaseSubject(FailureMetadata failureMetadata, SQLiteDatabase actual) {
        super(failureMetadata, actual);
    }

    public static SqliteDatabaseSubject assertThat(SQLiteDatabase db) {
        return assertAbout(SqliteDatabaseSubject::new).that(db);
    }

    public DatabaseTableSubject withTable(String table) {
        return check()
                .about((FailureMetadata failureMetadata, String actual) -> new DatabaseTableSubject(failureMetadata, actual, actual()))
                .that(table);
    }

    public static class DatabaseTableSubject extends Subject<DatabaseTableSubject, String> {
        private final SQLiteDatabase database;

        DatabaseTableSubject(FailureMetadata failureMetadata, String subject, SQLiteDatabase database) {
            super(failureMetadata, subject);

            this.database = database;
        }

        public void isEmpty() {
            int count = countRows();
            if (count > 0) {
                fail("is empty", count);
            }
        }

        public void isNotEmpty() {
            int count = countRows();
            if (count == 0) {
                fail("is not empty");
            }
        }

        public void hasPrimaryKey(String... keys) {
            check().that(getPrimaryKeys()).containsExactly((Object[]) keys);
        }

        private List<String> getPrimaryKeys() {
            try (Cursor cursor = database.rawQuery(String.format("PRAGMA table_info('%s')", actual()), null)) {
                List<String> columns = new ArrayList<>();
                while (cursor.moveToNext()) {
                    int pk = cursor.getInt(cursor.getColumnIndex("pk"));
                    if(pk > 0) {
                        columns.add(cursor.getString(cursor.getColumnIndex("name")));
                    }
                }
                return columns;
            }
        }

        private int countRows() {
            try (Cursor cursor = database.rawQuery(String.format("SELECT count(*) FROM %s", actual()), null)) {
                return cursor.moveToFirst() ? cursor.getInt(0) : 0;
            }
        }
    }
}
