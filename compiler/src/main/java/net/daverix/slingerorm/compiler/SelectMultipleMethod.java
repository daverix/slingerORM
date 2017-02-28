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

package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class SelectMultipleMethod implements StorageMethod {
    private final String methodName;
    private final String returnTypeName;
    private final String parameterText;
    private final String where;
    private final Collection<String> whereArgs;
    private final String orderBy;
    private final MapperDescription mapperDescription;

    SelectMultipleMethod(String methodName,
                         String returnTypeName,
                         String parameterText,
                         String where,
                         Collection<String> whereArgs,
                         String orderBy,
                         MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.returnTypeName = returnTypeName;
        this.parameterText = parameterText;
        this.where = where;
        this.whereArgs = whereArgs;
        this.orderBy = orderBy;
        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        String where = getWhere();
        String args = createArguments();
        String orderByText = createOrderBy();

        writer.write("    @Override\n");
        writer.write("    public " + returnTypeName + " " + methodName + "(" + parameterText + ") {\n");
        writer.write("        Cursor cursor = null;\n");
        writer.write("        try {\n");
        writer.write("            cursor = db.query(false,\n");
        writer.write("                    " + mapperDescription.getVariableName() + ".getTableName(),\n");
        writer.write("                    " + mapperDescription.getVariableName() + ".getFieldNames(),\n");
        writer.write("                    " + where + ",\n");
        writer.write("                    " + args + ",\n");
        writer.write("                    null,\n");
        writer.write("                    null,\n");
        writer.write("                    " + orderByText + ",\n");
        writer.write("                    null);\n\n");
        writer.write("            return " + mapperDescription.getVariableName() + ".mapList(cursor);\n");
        writer.write("        } finally {\n");
        writer.write("            if (cursor != null) cursor.close();\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String getWhere() {
        if(where == null) return "null";

        return "\"" + where + "\"";
    }

    private String createOrderBy() {
        if(orderBy == null) return "null";

        return "\"" + orderBy + "\"";
    }

    private String createArguments() {
        if(whereArgs == null || whereArgs.isEmpty()) return "null";

        return "new String[]{" + String.join(", ", whereArgs) +  "}";
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList(
                "android.database.Cursor",
                "android.database.sqlite.SQLiteDatabase",
                "java.util.List"
        );
    }

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
