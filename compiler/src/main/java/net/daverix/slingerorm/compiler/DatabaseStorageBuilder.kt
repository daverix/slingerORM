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
import java.util.*

class DatabaseStorageBuilder(private val writer: Writer) {
    private val storageMethods = HashSet<StorageMethod>()
    private var className: String? = null
    private var packageName: String? = null
    private var storageInterfaceName: String? = null

    fun setPackage(packageName: String?): DatabaseStorageBuilder {
        if (packageName == null) throw IllegalArgumentException("qualifiedName is null")

        this.packageName = packageName
        return this
    }

    fun setClassName(className: String?): DatabaseStorageBuilder {
        if (className == null) throw IllegalArgumentException("name is null")

        this.className = className
        return this
    }

    fun setStorageInterfaceName(storageInterfaceName: String?): DatabaseStorageBuilder {
        if (storageInterfaceName == null)
            throw IllegalArgumentException("storageInterfaceName is null")

        this.storageInterfaceName = storageInterfaceName
        return this
    }

    @Throws(IOException::class)
    fun addMethods(storageMethods: Iterable<StorageMethod>?): DatabaseStorageBuilder {
        if (storageMethods == null) throw IllegalArgumentException("storageMethods is null")

        for (method in storageMethods) {
            addMethod(method)
        }
        return this
    }

    @Throws(IOException::class)
    private fun addMethod(storageMethod: StorageMethod?): DatabaseStorageBuilder {
        if (storageMethod == null) throw IllegalArgumentException("storageMethod is null")

        storageMethods.add(storageMethod)
        return this
    }

    @Throws(IOException::class)
    fun build() {
        if (packageName == null)
            throw IllegalStateException("qualifiedName must be set")
        if (className == null)
            throw IllegalStateException("name must be set")
        if (storageInterfaceName == null)
            throw IllegalStateException("storageInterface must be set")

        writePackage()

        writeImports()
        writeClass()
    }

    @Throws(IOException::class)
    private fun writeClass() {
        val mapperVariables = storageMethods.distinctBy { it.mapperVariableName }
                .sortedBy { it.mapperVariableName }

        writer.write("public class $className implements $storageInterfaceName {\n")
        writer.write("    private final Database db;\n")
        mapperVariables.forEach {
            writer.write("    private final Mapper<${it.databaseEntityTypeName}> ${it.mapperVariableName};\n")
        }
        writeln()

        writer.write("    private $className(Builder builder) {\n")
        writer.write("        this.db = builder.db;\n")
        mapperVariables.forEach {
            writer.write("        this.${it.mapperVariableName} = builder.${it.mapperVariableName};\n")
        }
        writer.write("    }\n")
        writeln()

        writeMethods()

        writer.write("    public static Builder builder() {\n")
        writer.write("        return new Builder();\n")
        writer.write("    }\n")
        writeln()

        writeStorageBuilder(mapperVariables)

        writer.write("}\n")
    }


    @Throws(IOException::class)
    private fun writeMethods() {
        storageMethods.forEach { it.write(writer) }
    }

    @Throws(IOException::class)
    private fun writePackage() {
        writer.write("package $packageName;\n")
        writeln()
    }

    @Throws(IOException::class)
    private fun writeImports() {
        val qualifiedNames = HashSet<String>().apply {
            add("net.daverix.slingerorm.android.Database")
            add("net.daverix.slingerorm.android.SQLiteDatabaseWrapper")
            add("android.database.sqlite.SQLiteDatabase")
            add("net.daverix.slingerorm.android.Mapper")
        }

        storageMethods.forEach {
            qualifiedNames.addAll(it.imports)
            qualifiedNames.add(it.mapperQualifiedName)
        }

        qualifiedNames.remove("")
        qualifiedNames.sorted().forEach {
            writeImport(it)
        }
        writeln()
    }

    @Throws(IOException::class)
    private fun writeImport(importPackageName: String) {
        writer.write("import $importPackageName;\n")
    }

    @Throws(IOException::class)
    private fun writeln() {
        writer.write("\n")
    }

    @Throws(IOException::class)
    private fun writeStorageBuilder(mapperVariables: Iterable<StorageMethod>) {
        writer.write("    public static final class Builder {\n")
        writer.write("        private Database db;\n")

        mapperVariables.forEach {
            writer.write("        private Mapper<${it.databaseEntityTypeName}> ${it.mapperVariableName};\n")
        }
        writeln()
        writer.write("        private Builder() {\n")
        writer.write("        }\n")
        writeln()

        writer.write("        public Builder database(Database db) {\n")
        writer.write("            if (db == null)\n")
        writer.write("                throw new IllegalArgumentException(\"db is null\");\n\n")
        writer.write("            this.db = db;\n")
        writer.write("            return this;\n")
        writer.write("        }\n")
        writeln()

        writer.write("        public Builder database(SQLiteDatabase db) {\n")
        writer.write("            if (db == null)\n")
        writer.write("                throw new IllegalArgumentException(\"db is null\");\n\n")
        writer.write("            this.db = new SQLiteDatabaseWrapper(db);\n")
        writer.write("            return this;\n")
        writer.write("        }\n")
        writeln()

        mapperVariables.forEach { storageMethod ->
            writer.write("        public Builder ${storageMethod.mapperVariableName}(Mapper<${storageMethod.databaseEntityTypeName}> ${storageMethod.mapperVariableName}) {\n")
            writer.write("            this.${storageMethod.mapperVariableName} = ${storageMethod.mapperVariableName};\n")
            writer.write("            return this;\n")
            writer.write("        }\n")
            writeln()
        }

        writer.write("        public $storageInterfaceName build() {\n")
        writer.write("            if (db == null)\n")
        writer.write("                throw new IllegalStateException(\"database must be set\");\n")
        writeln()

        mapperVariables.forEach {
            writer.write("            if (${it.mapperVariableName} == null)\n")
            if (!it.mapperHasDependencies) {
                writer.write("                ${it.mapperVariableName} = ${it.databaseEntityTypeName}Mapper.create();\n")
            } else {
                writer.write("                throw new IllegalStateException(\"${it.mapperVariableName} must be set using ${it.mapperVariableName}()\");\n")
            }
            writeln()
        }

        writer.write("            return new $className(this);\n")
        writer.write("        }\n")

        writer.write("    }\n")
    }

    companion object {
        fun builder(writer: Writer?): DatabaseStorageBuilder {
            if (writer == null) throw IllegalArgumentException("writer is null")

            return DatabaseStorageBuilder(writer)
        }
    }
}

