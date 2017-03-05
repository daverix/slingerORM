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
import java.util.Collection;
import java.util.Collections;

class DeleteWhereMethod implements StorageMethod {
    private final String methodName;
    private final boolean returnDeleted;
    private final String parameterText;
    private final String where;
    private final Collection<String> whereArgs;
    private final MapperDescription mapperDescription;

    DeleteWhereMethod(String methodName,
                      boolean returnDeleted,
                      String parameterText,
                      String where,
                      Collection<String> whereArgs,
                      MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.returnDeleted = returnDeleted;
        this.parameterText = parameterText;
        this.where = where;
        this.whereArgs = whereArgs;

        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if(writer == null) throw new IllegalArgumentException("writer is null");

        String where = getWhere();
        String args = createArguments();
        String returnType = returnDeleted ? "int" : "void";

        //TODO: check for null in parameters?
        writer.write("    @Override\n");
        writer.write("    public " + returnType + " " + methodName + "(" + parameterText + ") {\n");
        writer.write("        " + (returnDeleted ? "return " : "") +  "db.delete(" + mapperDescription.getVariableName() + ".getTableName(),\n");
        writer.write("                " + where + ",\n");
        writer.write("                " + args + ");\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String getWhere() {
        if(where == null) return "null";

        return "\"" + where + "\"";
    }

    private String createArguments() {
        if(whereArgs == null || whereArgs.isEmpty()) return "null";

        return "new String[]{" + String.join(", ", whereArgs) +  "}";
    }

    @Override
    public Collection<String> getImports() {
        return Collections.emptyList();
    }

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
