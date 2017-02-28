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
import java.util.List;

class SelectSingleMethod implements StorageMethod {
    private final String methodName;
    private final String returnValue;
    private final String parameters;
    private final String where;
    private final List<String> whereArgs;
    private final MapperDescription mapperDescription;

    SelectSingleMethod(String methodName,
                       String returnValue,
                       String parameters,
                       String where,
                       List<String> whereArgs,
                       MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.returnValue = returnValue;
        this.parameters = parameters;
        this.where = where;
        this.whereArgs = whereArgs;
        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        String args = createArguments();

        writer.write("    @Override\n");
        writer.write("    public " + returnValue + " " + methodName + "(" + parameters + ") {\n");
        writer.write("        Cursor cursor = null;\n");
        writer.write("        try {\n");
        writer.write("            cursor = db.query(false,\n");
        writer.write("                    " + mapperDescription.getVariableName() + ".getTableName(),\n");
        writer.write("                    " + mapperDescription.getVariableName() + ".getFieldNames(),\n");
        writer.write("                    \"" + where + "\",\n");
        writer.write("                    " + args + ",\n");
        writer.write("                    null,\n");
        writer.write("                    null,\n");
        writer.write("                    null,\n");
        writer.write("                    \"1\");\n\n");
        writer.write("            if (!cursor.moveToFirst()) return null;\n");
        writer.write("            \n");
        writer.write("            return " + mapperDescription.getVariableName() + ".mapItem(cursor);\n");
        writer.write("        } finally {\n");
        writer.write("            if (cursor != null) cursor.close();\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String createArguments() {
        return "new String[]{" + String.join(", ", whereArgs) +  "}";
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList(
                "android.database.Cursor",
                "android.database.sqlite.SQLiteDatabase"
        );
    }

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
