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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DatabaseEntityMapperBuilder {
    private final Writer writer;
    private List<SerializerType> serializers;
    private String databaseEntityClassName;
    private String packageName;
    private String createTableSql;
    private String tableName;
    private String[] fieldNames;
    private List<FieldMethod> getters;
    private List<FieldMethod> setters;
    private String itemSql;
    private List<String> itemSqlArguments;

    private DatabaseEntityMapperBuilder(Writer writer) {
        this.writer = writer;
    }

    static DatabaseEntityMapperBuilder builder(Writer writer) {
        return new DatabaseEntityMapperBuilder(writer);
    }

    DatabaseEntityMapperBuilder setDatabaseEntityClassName(String databaseEntityClassName) {
        this.databaseEntityClassName = databaseEntityClassName;
        return this;
    }

    DatabaseEntityMapperBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    DatabaseEntityMapperBuilder setCreateTableSql(String createTableSql) {
        this.createTableSql = createTableSql;
        return this;
    }

    DatabaseEntityMapperBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    DatabaseEntityMapperBuilder setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
        return this;
    }

    DatabaseEntityMapperBuilder setGetters(List<FieldMethod> getters) {
        this.getters = getters;
        return this;
    }

    DatabaseEntityMapperBuilder setSetters(List<FieldMethod> setters) {
        this.setters = setters;
        return this;
    }

    DatabaseEntityMapperBuilder setItemSql(String itemSql) {
        this.itemSql = itemSql;
        return this;
    }

    DatabaseEntityMapperBuilder setItemSqlArguments(List<String> itemSqlArguments) {
        this.itemSqlArguments = itemSqlArguments;
        return this;
    }

    DatabaseEntityMapperBuilder setSerializers(List<SerializerType> serializers) {
        this.serializers = serializers;
        return this;
    }

    void build() throws IOException {
        if(databaseEntityClassName == null)
            throw new IllegalStateException("databaseEntityClassName not set");

        if(packageName == null)
            throw new IllegalStateException("packageName not set");

        if(createTableSql == null)
            throw new IllegalStateException("createTableSql not set");

        if(tableName == null)
            throw new IllegalStateException("tableName not set");

        if(fieldNames == null)
            throw new IllegalStateException("fieldNames not set");

        if(getters == null)
            throw new IllegalStateException("getters not set");

        if(setters == null)
            throw new IllegalStateException("setters not set");

        if(itemSql == null)
            throw new IllegalStateException("itemSql not set");

        if(itemSqlArguments == null)
            throw new IllegalStateException("itemSqlArguments not set");

        if(serializers == null)
            throw new IllegalStateException("serializers not set");

        writePackage();
        writeImports();
        writeClass();
    }

    private void writeClass() throws IOException {
        writer.write("public class " + databaseEntityClassName + "Mapper implements Mapper<" + databaseEntityClassName + "> {\n");
        for (int i = 0; i < serializers.size(); i++) {
            SerializerType serializer = serializers.get(i);
            writer.write("    private final " + serializer.getType() + " " + serializer.getName() + ";\n");
        }
        writeln();

        writer.write("    private " + databaseEntityClassName + "Mapper(" + (!serializers.isEmpty() ? "Builder builder" : "") + ") {\n");
        for (int i = 0; i < serializers.size(); i++) {
            SerializerType serializer = serializers.get(i);
            writer.write("        this." + serializer.getName() + " = builder." + serializer.getName() + ";\n");
        }
        writer.write("    }\n");
        writeln();

        writeMethods();

        writeBuilder();

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
        writer.write("        return new String[] { " + String.join(", ", (CharSequence[]) getCitedFieldNames()) + " };\n");
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
        writer.write("    public List<" + databaseEntityClassName + "> mapList(Cursor cursor) {\n");
        writer.write("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");\n");
        writeln();
        writer.write("        List<" + databaseEntityClassName + "> items = new ArrayList<" + databaseEntityClassName + ">();\n");
        writer.write("        while(cursor.moveToNext()) {\n");
        writer.write("            items.add(mapItem(cursor));\n");
        writer.write("        }\n");
        writer.write("        return items;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getItemQuery() {\n");
        writer.write("        return \"" + itemSql + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getItemQueryArguments(" + databaseEntityClassName + " item) {\n");
        writer.write("        return new String[]{\n");
        for (int i = 0; i < itemSqlArguments.size(); i++) {
            if (i == itemSqlArguments.size() - 1)
                writer.write("                " + itemSqlArguments.get(i) + "\n");
            else
                writer.write("                " + itemSqlArguments.get(i) + ",\n");
        }
        writer.write("        };\n");
        writer.write("    }\n");
        writeln();
    }

    private void writeBuilder() throws IOException {
        if(serializers.isEmpty()) {
            writer.write("    public static " + databaseEntityClassName + "Mapper create() {\n");
            writer.write("        return new " + databaseEntityClassName + "Mapper();\n");
            writer.write("    }\n");
        }
        writeln();

        writer.write("    public static Builder builder() {\n");
        writer.write("        return new Builder();\n");
        writer.write("    }\n");
        writeln();

        writer.write("    public static class Builder {\n");
        writer.write("        private Builder() {\n");
        writer.write("        }\n");
        writeln();

        for (int i = 0; i < serializers.size(); i++) {
            SerializerType serializer = serializers.get(i);
            writer.write("        private " + serializer.getType() + " " + serializer.getName() +";\n");
        }
        writeln();

        for (int i = 0; i < serializers.size(); i++) {
            SerializerType serializer = serializers.get(i);
            writer.write("        public Builder " + serializer.getName() + "(" + serializer.getType() + " " + serializer.getName() +") {\n");
            writer.write("            if (" + serializer.getName() + " == null)\n");
            writer.write("                throw new IllegalArgumentException(\"" + serializer.getName() + " is null\");\n");
            writeln();
            writer.write("            this." + serializer.getName() + " = " + serializer.getName() + ";\n");
            writer.write("            return this;\n");
            writer.write("        }\n");

            writeln();
        }

        writer.write("        public " + databaseEntityClassName + "Mapper build() {\n");
        for (int i = 0; i < serializers.size(); i++) {
            SerializerType serializer = serializers.get(i);
            writer.write("            if (" + serializer.getName() + " == null)\n");
            writer.write("                throw new IllegalStateException(\"" + serializer.getName() + " is not set\");\n");
            writeln();
        }

        if(serializers.isEmpty()) {
            writer.write("            return new " + databaseEntityClassName + "Mapper();\n");
        } else {
            writer.write("            return new " + databaseEntityClassName + "Mapper(this);\n");
        }

        writer.write("        }\n");
        writer.write("    }\n");
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
        Set<String> qualifiedNames = new HashSet<>();
        qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
        qualifiedNames.add("android.content.ContentValues");
        qualifiedNames.add("android.database.Cursor");
        qualifiedNames.add("java.util.List");
        qualifiedNames.add("java.util.ArrayList");
        for (int i = 0; i < serializers.size(); i++) {
            qualifiedNames.addAll(serializers.get(i).getImports());
        }

        List<String> sortedNames = new ArrayList<>(qualifiedNames);
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
