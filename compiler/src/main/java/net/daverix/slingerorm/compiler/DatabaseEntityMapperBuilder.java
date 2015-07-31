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

import net.daverix.slingerorm.serialization.DefaultSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseEntityMapperBuilder {
    private final Writer writer;
    private String databaseEntityClassName;
    private String serializerClassName;
    private String serializerQualifiedName;
    private String packageName;
    private String createTableSql;
    private String tableName;
    private String[] fieldNames;
    private List<FieldMethod> getters;
    private List<FieldMethod> setters;
    private String itemSql;
    private String itemSqlArguments;

    private DatabaseEntityMapperBuilder(Writer writer) {
        this.writer = writer;
    }

    public static DatabaseEntityMapperBuilder builder(Writer writer) {
        return new DatabaseEntityMapperBuilder(writer);
    }

    public DatabaseEntityMapperBuilder setSerializerQualifiedName(String serializerQualifiedName) {
        this.serializerQualifiedName = serializerQualifiedName;
        return this;
    }
    public DatabaseEntityMapperBuilder setSerializerClassName(String serializerClassName) {
        this.serializerClassName = serializerClassName;
        return this;
    }

    public DatabaseEntityMapperBuilder setDatabaseEntityClassName(String databaseEntityClassName) {
        this.databaseEntityClassName = databaseEntityClassName;
        return this;
    }
    public DatabaseEntityMapperBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public DatabaseEntityMapperBuilder setCreateTableSql(String createTableSql) {
        this.createTableSql = createTableSql;
        return this;
    }

    public DatabaseEntityMapperBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public DatabaseEntityMapperBuilder setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
        return this;
    }

    public DatabaseEntityMapperBuilder setGetters(List<FieldMethod> getters) {
        this.getters = getters;
        return this;
    }

    public DatabaseEntityMapperBuilder setSetters(List<FieldMethod> setters) {
        this.setters = setters;
        return this;
    }

    public DatabaseEntityMapperBuilder setItemSql(String itemSql) {
        this.itemSql = itemSql;
        return this;
    }

    public DatabaseEntityMapperBuilder setItemSqlArguments(String itemSqlArguments) {
        this.itemSqlArguments = itemSqlArguments;
        return this;
    }

    public void build() throws IOException {
        writePackage();
        writeImports();
        writeClass();
    }

    private void writeClass() throws IOException {
        writer.write("public class " + databaseEntityClassName + "Mapper extends Mapper<" + databaseEntityClassName + "> {\n");
        if(!DefaultSerializer.class.getCanonicalName().equals(serializerQualifiedName)) {
            writer.write("    private final " + serializerClassName + " serializer;\n");
        }
        writeln();

        if(DefaultSerializer.class.getCanonicalName().equals(serializerQualifiedName)) {
            writer.write("    public " + databaseEntityClassName + "Mapper() {\n");
        }
        else {
            writer.write("    public " + databaseEntityClassName + "Mapper(" + serializerClassName + " serializer) {\n");
            writer.write("        if(serializer == null) throw new IllegalArgumentException(\"serializer is null\");\n");
            writer.write("        this.serializer = serializer;\n");
        }
        writer.write("    }\n");
        writeln();

        writeMethods();

        writer.write("}\n");
    }

    private void writeMethods() throws IOException {
        writer.write("    @Override\n");
        writer.write("    public String createTable() {\n");
        writer.write("        return \"" + createTableSql + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getTableName() {\n");
        writer.write("        return \"" + tableName + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getFieldNames() {\n");
        writer.write("        return new String[] { " + String.join(", ", getCitedFieldNames()) + " };\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public ContentValues mapValues(" + databaseEntityClassName + " item) {\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n");
        writeln();
        writer.write("        ContentValues values = new ContentValues();\n");
        for(FieldMethod getter : getters) {
            writer.write("        values.put(" + getter.getMethod() + ");\n");
        }
        writer.write("        return values;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public " + databaseEntityClassName + " mapItem(Cursor cursor) {\n");
        writer.write("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");\n");
        writeln();
        writer.write("        " + databaseEntityClassName + " item = new " + databaseEntityClassName + "();\n");
        for(FieldMethod setter : setters) {
            writer.write("        item." + setter.getMethod() + ";\n");
        }
        writer.write("        return item;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getItemQuery() {\n");
        writer.write("        return \"" + itemSql + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getItemQueryArguments(" + databaseEntityClassName + " item) {\n");
        writer.write("        return " + itemSqlArguments + ";\n");
        writer.write("    }\n");
        writeln();
    }

    private String[] getCitedFieldNames() {
        String[] cited = new String[fieldNames.length];
        for(int i=0;i<cited.length;i++) {
            cited[i] = "\"" + fieldNames[i] + "\"";
        }
        return cited;
    }

    private void writePackage() throws IOException {
        writer.write("package " + packageName + ";\n");
        writeln();
    }

    private void writeImports() throws IOException {
        Set<String> qualifiedNames = new HashSet<String>();
        qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
        qualifiedNames.add(serializerQualifiedName);
        qualifiedNames.add("android.content.ContentValues");
        qualifiedNames.add("android.database.Cursor");
        qualifiedNames.add("java.util.List");
        qualifiedNames.add("java.util.ArrayList");

        List<String> sortedNames = new ArrayList<String>(qualifiedNames);
        Collections.sort(sortedNames);
        for(String qualifiedName : sortedNames) {
            writeImport(qualifiedName);
        }
        writeln();
    }

    private void writeImport(String importPackageName) throws IOException {
        writer.write(String.format("import %s;\n", importPackageName));
    }

    private void writeln() throws IOException {
        writer.write("\n");
    }
}
