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

package net.daverix.slingerorm.compiler

import java.io.IOException
import java.io.Writer

class SelectSingleMethod(private val methodName: String,
                         private val parameters: String,
                         private val where: String,
                         private val whereArgs: List<String>,
                         override val databaseEntityTypeName: String,
                         override val mapperQualifiedName: String,
                         override val mapperVariableName: String,
                         override val mapperHasDependencies: Boolean) : StorageMethod {

    @Throws(IOException::class)
    override fun write(writer: Writer) {
        val args = createArguments()

        writer.write("    @Override\n")
        writer.write("    public $databaseEntityTypeName $methodName($parameters) {\n")
        writer.write("        Cursor cursor = null;\n")
        writer.write("        try {\n")
        writer.write("            cursor = db.query(false,\n")
        writer.write("                    $mapperVariableName.getTableName(),\n")
        writer.write("                    $mapperVariableName.getFieldNames(),\n")
        writer.write("                    \"$where\",\n")
        writer.write("                    $args,\n")
        writer.write("                    null,\n")
        writer.write("                    null,\n")
        writer.write("                    null,\n")
        writer.write("                    \"1\");\n\n")
        writer.write("            if (!cursor.moveToFirst()) return null;\n")
        writer.write("            \n")
        writer.write("            return $mapperVariableName.mapItem(cursor);\n")
        writer.write("        } finally {\n")
        writer.write("            if (cursor != null) cursor.close();\n")
        writer.write("        }\n")
        writer.write("    }\n")
        writer.write("\n")
    }

    private fun createArguments(): String {
        return "new String[]{${whereArgs.joinToString(", ")}}"
    }

    override val imports
        get(): Collection<String> {
            return listOf("android.database.Cursor")
        }
}
