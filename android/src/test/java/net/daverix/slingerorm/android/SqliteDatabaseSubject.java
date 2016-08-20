/*
 * Copyright 2015 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.daverix.slingerorm.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.truth.FailureStrategy;
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
