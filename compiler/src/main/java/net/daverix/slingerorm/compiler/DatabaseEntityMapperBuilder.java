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

import net.daverix.slingerorm.compiler.mapping.Setter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseEntityMapperBuilder {
    private final Writer writer;

    public DatabaseEntityMapperBuilder(Writer writer) {
        this.writer = writer;
    }

    public void build(DatabaseEntityModel entityModel) throws IOException {
        writePackage(entityModel);
        writeImports(entityModel);
        writeClass(entityModel);
    }

    private void writeClass(DatabaseEntityModel entityModel) throws IOException {
        String mapperClassName = entityModel.getMapperClassName();
        String databaseEntityClassName = entityModel.getDatabaseEntityClassName();
        Collection<String> serializerFields = entityModel.getSerializerFieldNames();

        writer.write("@EntityMapper\n");
        writer.write("public class " + mapperClassName + " implements Mapper<" + databaseEntityClassName + "> {\n");
        for(String serializerField : serializerFields) {
            writeSerializerField(entityModel, serializerField);
        }
        writeln();

        writer.write("    public " + mapperClassName + "(" + getConstructorParameterText(entityModel) + ") {\n");
        for(String serializerField : serializerFields) {
            writeIllegalArgumentForSerializer(serializerField);
        }
        writeln();
        for(String serializerField : serializerFields) {
            writeSerializerFieldAllocation(serializerField);
        }
        writer.write("    }\n");
        writeln();

        writeMethods(entityModel);

        writer.write("}\n");
    }

    private String getConstructorParameterText(DatabaseEntityModel entityModel) {
        List<String> typeParameterNamePairs = new ArrayList<String>();
        Collection<String> serializerFields = entityModel.getSerializerFieldNames();
        for(String serializerFieldName : serializerFields) {
            String serializerClassName = entityModel.getSerializerClassName(serializerFieldName);
            typeParameterNamePairs.add(serializerClassName + " " + serializerFieldName);
        }
        return String.join(", ", typeParameterNamePairs);
    }

    private void writeIllegalArgumentForSerializer(String serializerField) throws IOException {
        writer.write("        if(" + serializerField + " == null) throw new IllegalArgumentException(\"" + serializerField + " is null\");\n");
    }

    private void writeSerializerFieldAllocation(String serializerField) throws IOException {
        writer.write("        this." + serializerField + " = " + serializerField + ";\n");
    }

    private void writeSerializerField(DatabaseEntityModel entityModel, String serializerField) throws IOException {
        String serializerClassName = entityModel.getSerializerClassName(serializerField);
        writer.write("    private final " + serializerClassName + " " + serializerField + ";\n");
    }

    private void writeMethods(DatabaseEntityModel entityModel) throws IOException {
        String databaseEntityClassName = entityModel.getDatabaseEntityClassName();

        writer.write("    @Override\n");
        writer.write("    public String createTable() {\n");
        writer.write("        return \"" + entityModel.getCreateTableSql() + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getTableName() {\n");
        writer.write("        return \"" + entityModel.getTableName() + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getColumnNames() {\n");
        writer.write("        return new String[] { " + String.join(", ", getCitedColumnNames(entityModel)) + " };\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");

        writer.write("    public ContentValues mapValues(" + databaseEntityClassName + " item) {\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n");
        writeln();
        writer.write("        ContentValues values = new ContentValues();\n");
        for(ColumnModel columnModel : entityModel.getColumns().values()) {
            writeValuesForColumn(columnModel, "item");
        }
        writer.write("        return values;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public " + databaseEntityClassName + " mapItem(Cursor cursor) {\n");
        writer.write("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");\n");
        writeln();
        writeFieldSetters(databaseEntityClassName, entityModel.getFields().values(), "item", "cursor");
        writer.write("        return item;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getItemQuery() {\n");
        writer.write("        return \"" + entityModel.getItemSql() + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getItemQueryArguments(" + databaseEntityClassName + " item) {\n");
        writer.write("        return new String[]{" + getItemSqlArgumentsText(entityModel) + "};\n");
        writer.write("    }\n");
        writeln();
    }

    private void writeFieldSetters(String databaseEntityClassName, Collection<FieldModel> fields, String variable, String cursorName) throws IOException {
        writer.write("        " + databaseEntityClassName + " " + variable + " = new " + databaseEntityClassName + "();\n");
        for(FieldModel fieldModel : fields) {
            if(fieldModel.isDatabaseEntity()) {
                writeFieldSetters(fieldModel.getDatabaseEntityModel().getDatabaseEntityClassName(),
                        fieldModel.getDatabaseEntityModel().getFields().values(),
                        fieldModel.getName(),
                        cursorName);
            }
            writeSetterForField(fieldModel.getSetter(), variable, cursorName);
        }
    }

    private String getItemSqlArgumentsText(DatabaseEntityModel entityModel) {
        List<String> getters = new ArrayList<>();
        for(String column : entityModel.getItemSqlArgColumns()) {
            String getter = entityModel.getColumns().get(column).getGetter().get("item");
            if(entityModel.isFieldString(column)) {
                getters.add(getter);
            }
            else {
                getters.add(String.format("String.valueOf(item.%s)", getter));
            }
        }
        return String.join(", ", getters);
    }

    private void writeValuesForColumn(ColumnModel columnModel, String variable) throws IOException {
        writeValues(columnModel.getName(), columnModel.getGetter().get(variable));
    }

    private void writeValues(String key, String value) throws IOException {
        writer.write("        values.put(\"" + key + "\", " + value + ");\n");
    }

    private void writeSetterForField(Setter setter, String itemVariable, String cursorVariable) throws IOException {
        writer.write("        " + setter.set(itemVariable, cursorVariable) + ";\n");
    }

    private List<String> getCitedColumnNames(DatabaseEntityModel entityModel) {
        Collection<String> columnNames = entityModel.getColumns().keySet();
        List<String> cited = new ArrayList<>();
        for(String columnName : columnNames) {
            cited.add("\"" + columnName + "\"");
        }
        return cited;
    }

    private void writePackage(DatabaseEntityModel entityModel) throws IOException {
        writer.write("package " + entityModel.getMapperPackageName() + ";\n");
        writeln();
    }

    private void writeImports(DatabaseEntityModel entityModel) throws IOException {
        Set<String> qualifiedNames = new HashSet<>();
        qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
        qualifiedNames.add("net.daverix.slingerorm.internal.EntityMapper");
        qualifiedNames.add("android.content.ContentValues");
        qualifiedNames.add("android.database.Cursor");
        qualifiedNames.addAll(entityModel.getQualifiedNamesForSerializers());

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
