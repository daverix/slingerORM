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

class DeleteWhereMethod(private val methodName: String,
                        private val returnDeleted: Boolean,
                        private val parameterText: String,
                        private val where: String?,
                        private val whereArgs: Collection<String>?,
                        override val databaseEntityTypeName: String,
                        override val mapperQualifiedName: String,
                        override val mapperVariableName: String,
                        override val mapperHasDependencies: Boolean) : StorageMethod {

    @Throws(IOException::class)
    override fun write(writer: Writer) {
        val where = getWhere()
        val args = createArguments()
        val returnType = if (returnDeleted) "int" else "void"

        //TODO: check for null in parameters?
        writer.write("    @Override\n")
        writer.write("    public $returnType $methodName($parameterText) {\n")
        writer.write("        ${if (returnDeleted) "return " else ""}db.delete($mapperVariableName.getTableName(),\n")
        writer.write("                $where,\n")
        writer.write("                $args);\n")
        writer.write("    }\n")
        writer.write("\n")
    }

    private fun getWhere(): String {
        return if (where == null) "null" else "\"" + where + "\""
    }

    private fun createArguments(): String {
        return if (whereArgs == null || whereArgs.isEmpty()) "null" else "new String[]{" + whereArgs.joinToString(", ") + "}"
    }

    override val imports get(): Collection<String> {
        return emptyList()
    }
}
