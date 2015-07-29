package net.daverix.slingerorm.android;

import android.database.sqlite.SQLiteDatabase;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

public class SqliteDatabaseSubject extends Subject<SqliteDatabaseSubject, SQLiteDatabase> {
    private static final SubjectFactory<SqliteDatabaseSubject, SQLiteDatabase> DATABASE_SUBJECT = new SubjectFactory<SqliteDatabaseSubject, SQLiteDatabase>() {
        @Override
        public SqliteDatabaseSubject getSubject(FailureStrategy fs, SQLiteDatabase that) {
            return new SqliteDatabaseSubject(fs, that);
        }
    };
    public static SubjectFactory<SqliteDatabaseSubject, SQLiteDatabase> database() {
        return DATABASE_SUBJECT;
    }

    public SqliteDatabaseSubject(FailureStrategy failureStrategy, SQLiteDatabase subject) {
        super(failureStrategy, subject);
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

        public IntegerSubject hasRowCountThat() {
            int count = database.query(false, getSubject(), null, null, null, null, null, null, null).getCount();
            return new IntegerSubject(failureStrategy, count);
        }
    }
}
