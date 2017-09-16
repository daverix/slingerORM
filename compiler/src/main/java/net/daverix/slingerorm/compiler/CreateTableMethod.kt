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

class CreateTableMethod(private val methodName: String,
                        override val mapperQualifiedName: String,
                        override val mapperVariableName: String,
                        override val databaseEntityTypeName: String,
                        override val mapperHasDependencies: Boolean) : StorageMethod {
    @Throws(IOException::class)
    override fun write(writer: Writer) {
        with(writer) {
            write("    @Override\n")
            write("    public void $methodName() {\n")
            write("        db.execSQL($mapperVariableName.createTable());\n")
            write("    }\n")
            write("\n")
        }
    }

    override val imports get() = emptyList<String>()
}
