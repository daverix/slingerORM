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

import android.content.ContentValues;
import android.database.Cursor;

public abstract class AbstractDatabaseProxy {
    public abstract void execSql(String sql);

    public abstract Cursor query(boolean distinct, String table, String[] columns,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit);

    public abstract void insert(String tableName, ContentValues values);
    public abstract void replace(String tableName, ContentValues values);
    public abstract int update(String tableName, ContentValues values, String where, String[] whereArgs);
    public abstract int delete(String tableName, String where, String[] whereArgs);

    public abstract void beginTransaction();
    public abstract void endTransaction();
    public abstract void setTransactionSuccessful();
}
