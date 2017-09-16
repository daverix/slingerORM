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
import kotlin.collections.ArrayList


class DatabaseEntityMapperBuilder constructor(private val writer: Writer) {
    var serializers: List<SerializerType> = ArrayList()
    var databaseEntityClassName: String = ""
    var packageName: String = ""
    var createTableSql: String = ""
    var tableName: String = ""
    var fieldNames: List<String> = ArrayList()
    var getters: List<FieldMethod> = ArrayList()
    var setters: List<FieldMethod> = ArrayList()
    var itemSql: String = ""
    var itemSqlArguments: List<String> = ArrayList()

    @Throws(IOException::class)
    fun build() {
        if (databaseEntityClassName.isEmpty())
            throw IllegalStateException("databaseEntityClassName not set")

        if (packageName.isEmpty())
            throw IllegalStateException("packageName not set")

        if (createTableSql.isEmpty())
            throw IllegalStateException("createTableSql not set")

        if (tableName.isEmpty())
            throw IllegalStateException("tableName not set")

        if (fieldNames.isEmpty())
            throw IllegalStateException("fieldNames not set")

        if (itemSql.isEmpty())
            throw IllegalStateException("itemSql not set")

        if (itemSqlArguments.isEmpty())
            throw IllegalStateException("itemSqlArguments not set")

        writePackage()
        writeImports()
        writeClass()
    }

    @Throws(IOException::class)
    private fun writeClass() {
        with(writer) {
            writeLine("public class ${databaseEntityClassName}Mapper implements Mapper<$databaseEntityClassName> {")
            writeLines(serializers) { (name, type) -> "    private final $type $name;" }
            writeLine()
            writeLine("    private ${databaseEntityClassName}Mapper(${if (serializers.isNotEmpty()) "Builder builder" else ""}) {")
            writeLines(serializers) { (name) -> "        this.$name = builder.$name;" }
            writeLine("    }")
            writeLine()

            writeMethods()

            writeBuilder()

            writer.write("}\n")
        }
    }

    @Throws(IOException::class)
    private fun writeMethods() {
        with(writer) {
            writeLine("    @Override")
            writeLine("    public String createTable() {")
            writeLine("        return \"$createTableSql\";")
            writeLine("    }")
            writeLine()

            writeLine("    @Override")
            writeLine("    public String getTableName() {")
            writeLine("        return \"$tableName\";")
            writeLine("    }")
            writeLine()

            writeLine("    @Override")
            writeLine("    public String[] getFieldNames() {")
            writeLine("        return new String[] { ${fieldNames.quote().joinToString(", ")} };")
            writeLine("    }")

            writeLine("    @Override")
            writeLine("    public ContentValues mapValues($databaseEntityClassName item) {")
            writeLine("        if(item == null) throw new IllegalArgumentException(\"item is null\");")
            writeLine()

            writeLine("        ContentValues values = new ContentValues();")
            writeLines(getters) { it -> "        values.put(${it.method});" }
            writeLine("        return values;")
            writeLine("    }")
            writeLine()

            writeLine("    @Override")
            writeLine("    public $databaseEntityClassName mapItem(Cursor cursor) {")
            writeLine("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");")
            writeLine()
            writeLine("        $databaseEntityClassName item = new $databaseEntityClassName();")
            writeLines(setters) { it -> "        item.${it.method};" }
            writeLine("        return item;")
            writeLine("    }")
            writeLine()

            writeLine("    @Override")
            writeLine("    public List<$databaseEntityClassName> mapList(Cursor cursor) {")
            writeLine("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");")
            writeLine()
            writeLine("        List<$databaseEntityClassName> items = new ArrayList<$databaseEntityClassName>();")
            writeLine("        while(cursor.moveToNext()) {")
            writeLine("            items.add(mapItem(cursor));")
            writeLine("        }")
            writeLine("        return items;")
            writeLine("    }")

            writeLine("    @Override")
            writeLine("    public String getItemQuery() {")
            writeLine("        return \"$itemSql\";")
            writeLine("    }")
            writeLine()

            writeLine("    @Override\n")
            writeLine("    public String[] getItemQueryArguments($databaseEntityClassName item) {\n")
            writeLine("        return new String[]{\n")
            writeLines(itemSqlArguments) { i, it ->
                "                $it${if (i == itemSqlArguments.size - 1) "" else ","}"
            }
            writeLine("        };")
            writeLine("    }")
            writeLine()
        }
    }

    @Throws(IOException::class)
    private fun writeBuilder() {
        with(writer) {
            if (serializers.isEmpty()) {
                writeLine("    public static ${databaseEntityClassName}Mapper create() {")
                writeLine("        return new ${databaseEntityClassName}Mapper();")
                writeLine("    }\n")
                writeLine()
            }

            writeLine("    public static Builder builder() {")
            writeLine("        return new Builder();")
            writeLine("    }")
            writeLine()

            writeLine("    public static class Builder {")
            writeLine("        private Builder() {")
            writeLine("        }")
            writeLine()
            writeLines(serializers) { (name, type) -> "        private $type $name;" }
            writeLine()

            serializers.forEach { (name, type) ->
                writeLine("        public Builder $name($type $name) {")
                writeLine("            if ($name == null)")
                writeLine("                throw new IllegalArgumentException(\"$name is null\");")
                writeLine()
                writeLine("            this.$name = $name;")
                writeLine("            return this;")
                writeLine("        }")
                writeLine()
            }

            writeLine("        public ${databaseEntityClassName}Mapper build() {")
            serializers.forEach { (name) ->
                writeLine("            if ($name == null)")
                writeLine("                throw new IllegalStateException(\"$name is not set\");")
                writeLine()
            }

            if (serializers.isEmpty()) {
                writeLine("            return new ${databaseEntityClassName}Mapper();\n")
            } else {
                writeLine("            return new ${databaseEntityClassName}Mapper(this);\n")
            }

            writeLine("        }")
            writeLine("    }")
        }
    }

    @Throws(IOException::class)
    private fun writePackage() {
        writer.write("package $packageName;\n")
        writeln()
    }

    @Throws(IOException::class)
    private fun writeImports() {
        val qualifiedNames = HashSet<String>()
        qualifiedNames += "net.daverix.slingerorm.android.Mapper"
        qualifiedNames += "android.content.ContentValues"
        qualifiedNames += "android.database.Cursor"
        qualifiedNames += "java.util.List"
        qualifiedNames += "java.util.ArrayList"
        qualifiedNames += serializers.flatMap { serializer -> serializer.imports }

        qualifiedNames.sorted().forEach {
            writer.writeLine("import $it;")
        }
        writer.writeLine()
    }

    @Throws(IOException::class)
    private fun writeln() {
        writer.write("\n")
    }

    companion object {

        fun builder(writer: Writer): DatabaseEntityMapperBuilder {
            return DatabaseEntityMapperBuilder(writer)
        }
    }
}
