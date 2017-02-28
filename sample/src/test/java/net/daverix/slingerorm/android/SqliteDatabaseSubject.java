package net.daverix.slingerorm.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import static com.google.common.truth.Truth.assertAbout;

public class SqliteDatabaseSubject extends Subject<SqliteDatabaseSubject, SQLiteDatabase> {
    private static final SubjectFactory<SqliteDatabaseSubject, SQLiteDatabase> DATABASE_SUBJECT = new SubjectFactory<SqliteDatabaseSubject, SQLiteDatabase>() {
        @Override
        public SqliteDatabaseSubject getSubject(FailureStrategy fs, SQLiteDatabase that) {
            return new SqliteDatabaseSubject(fs, that);
        }
    };

    private SqliteDatabaseSubject(FailureStrategy failureStrategy, SQLiteDatabase subject) {
        super(failureStrategy, subject);
    }

    public static SqliteDatabaseSubject assertThat(SQLiteDatabase db) {
        return assertAbout(DATABASE_SUBJECT).that(db);
    }

    public DatabaseTableSubject withTable(String table) {
        return new DatabaseTableSubject(failureStrategy, table, getSubject());
    }

    public static class DatabaseTableSubject extends Subject<DatabaseTableSubject,String> {
        private final SQLiteDatabase database;

        public DatabaseTableSubject(FailureStrategy failureStrategy, String subject, SQLiteDatabase database) {
            super(failureStrategy, subject);

            this.database = database;
        }

        public void isEmpty() {
            Cursor cursor = null;
            try {
                cursor = database.query(false, getSubject(), null, null, null, null, null, null, null);

                if(cursor.getCount() > 0) {
                    fail(String.format("is not empty (got %d items)", cursor.getCount()));
                }
            } finally {
                if(cursor != null) cursor.close();
            }

        }

        public void isNotEmpty() {
            Cursor cursor = null;
            try {
                cursor = database.query(false, getSubject(), null, null, null, null, null, null, null);

                if(cursor.getCount() == 0) {
                    fail("is empty");
                }
            } finally {
                if(cursor != null) cursor.close();
            }
        }
    }
}
