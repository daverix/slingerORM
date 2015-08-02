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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseEntityMapperBuilder {
    private final Writer writer;
    private final DatabaseEntityModel entityModel;

    public DatabaseEntityMapperBuilder(Writer writer, DatabaseEntityModel entityModel) {
        this.writer = writer;
        this.entityModel = entityModel;
    }

    public void build() throws IOException {
        writePackage();
        writeImports();
        writeClass();
    }

    private void writeClass() throws IOException {
        String mapperClassName = entityModel.getMapperClassName();
        String databaseEntityClassName = entityModel.getDatabaseEntityClassName();
        Collection<String> serializerFields = entityModel.getSerializerFieldNames();

        writer.write("public class " + mapperClassName + " extends Mapper<" + databaseEntityClassName + "> {\n");
        for(String serializerField : serializerFields) {
            writeSerializerField(serializerField);
        }
        writeln();

        writer.write("    public " + databaseEntityClassName + "Mapper(" + getConstructorParameterText() + ") {\n");
        for(String serializerField : serializerFields) {
            writeIllegalArgumentForSerializer(serializerField);
        }
        writeln();
        for(String serializerField : serializerFields) {
            writeSerializerFieldAllocation(serializerField);
        }
        writer.write("    }\n");
        writeln();

        writeMethods();

        writer.write("}\n");
    }

    private String getConstructorParameterText() {
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

    private void writeSerializerField(String serializerField) throws IOException {
        String serializerClassName = entityModel.getSerializerClassName(serializerField);
        writer.write("    private final " + serializerClassName + " " + serializerField + ";\n");
    }

    private void writeMethods() throws IOException {
        Set<String> fields = entityModel.getFieldNames();

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
        writer.write("        return new String[] { " + String.join(", ", getCitedColumnNames()) + " };\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public ContentValues mapValues(" + entityModel.getDatabaseEntityClassName() + " item) {\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n");
        writeln();
        writer.write("        ContentValues values = new ContentValues();\n");
        for(String field : fields) {
            writeValuesForField(field);
        }
        writer.write("        return values;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public " + entityModel.getDatabaseEntityClassName() + " mapItem(Cursor cursor) {\n");
        writer.write("        if(cursor == null) throw new IllegalArgumentException(\"cursor is null\");\n");
        writeln();
        writer.write("        " + entityModel.getDatabaseEntityClassName() + " item = new " + entityModel.getDatabaseEntityClassName() + "();\n");
        for(String field : fields) {
            writeSetterForField(field);
        }
        writer.write("        return item;\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String getItemQuery() {\n");
        writer.write("        return \"" + entityModel.getItemSql() + "\";\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public String[] getItemQueryArguments(" + entityModel.getDatabaseEntityClassName() + " item) {\n");
        writer.write("        return new String[]{" + getItemSqlArgumentsText() + "};\n");
        writer.write("    }\n");
        writeln();
    }

    private String getItemSqlArgumentsText() {
        List<String> getters = new ArrayList<String>();
        for(String field : entityModel.getItemSqlArgFields()) {
            if(entityModel.isFieldString(field)) {
                getters.add("item." + getEntityFieldMethod(field));
            }
            else {
                getters.add("String.valueOf(item." + getEntityFieldMethod(field) + ")");
            }
        }
        return String.join(", ", getters);
    }

    private void writeValuesForField(String field) throws IOException {
        String columnName = entityModel.getColumnName(field);

        if(entityModel.hasSerializer(field)) {
            String serializerFieldName = entityModel.getSerializerFieldName(field);
            writeValues(columnName, serializerFieldName + ".serialize(item." + getEntityFieldMethod(field) + ")");
        }
        else {
            writeValues(columnName, "item." + getEntityFieldMethod(field));
        }
    }

    private String getEntityFieldMethod(String field) {
        FieldAccess fieldAccess = entityModel.getGetFieldAccess(field);
        switch (fieldAccess) {
            case ANNOTATED_METHOD:
                return entityModel.getGetFieldAnnotatedMethod(field) + "()";
            case STANDARD_METHOD:
                return entityModel.getStandardGetMethod(field) + "()";
            case FIELD:
                return field;
            default:
                throw new IllegalStateException("unknown type!");
        }
    }

    private void writeValues(String key, String value) throws IOException {
        writer.write("        values.put(\"" + key + "\", " + value + ");\n");
    }

    private void writeSetterForField(String field) throws IOException {
        String method;
        if(entityModel.hasSerializer(field)) {
            String serializerField = entityModel.getSerializerFieldName(field);
            method = getSetMethod(field, serializerField + ".deserialize(" + getCursorMethod(field)) + ")";
        }
        else {
            method = getSetMethod(field, getCursorMethod(field));
        }

        writer.write("        item." + method + ";\n");
    }

    private String getSetMethod(String field, String cursorValue) {
        FieldAccess fieldAccess = entityModel.getSetFieldAccess(field);
        switch (fieldAccess) {
            case ANNOTATED_METHOD:
                return entityModel.getSetFieldAnnotatedMethod(field) + "(" + cursorValue + ")";
            case STANDARD_METHOD:
                return entityModel.getStandardSetMethod(field) + "(" + cursorValue + ")";
            case FIELD:
                return field + " = " + cursorValue;
            default:
                throw new IllegalStateException("unknown type!");
        }
    }

    private String getCursorMethod(String field) {
        CursorType cursorType = entityModel.getCursorType(field);
        switch (cursorType) {
            case BYTE_ARRAY:
                return "cursor.getBlob(" + getColumnIndex(field) + ")";
            case SHORT:
                return "cursor.getShort(" + getColumnIndex(field) + ")";
            case INT:
                return "cursor.getInt(" + getColumnIndex(field) + ")";
            case LONG:
                return "cursor.getLong(" + getColumnIndex(field) + ")";
            case FLOAT:
                return "cursor.getFloat(" + getColumnIndex(field) + ")";
            case DOUBLE:
                return "cursor.getDouble(" + getColumnIndex(field) + ")";
            case STRING:
                return "cursor.getString(" + getColumnIndex(field) + ")";
            case BOOLEAN:
                return "cursor.getShort(" + getColumnIndex(field) + ") == 1";
            default:
                throw new IllegalStateException("unknown type");
        }
    }

    private String getColumnIndex(String field) {
        String columnName = entityModel.getColumnName(field);
        return "cursor.getColumnIndex(\"" + columnName + "\")";
    }

    private List<String> getCitedColumnNames() {
        Collection<String> columnNames = entityModel.getColumnNames();
        List<String> cited = new ArrayList<String>();
        for(String columnName : columnNames) {
            cited.add("\"" + columnName + "\"");
        }
        return cited;
    }

    private void writePackage() throws IOException {
        writer.write("package " + entityModel.getMapperPackageName() + ";\n");
        writeln();
    }

    private void writeImports() throws IOException {
        Set<String> qualifiedNames = new HashSet<String>();
        qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
        qualifiedNames.add("android.content.ContentValues");
        qualifiedNames.add("android.database.Cursor");
        qualifiedNames.add("java.util.List");
        qualifiedNames.add("java.util.ArrayList");
        qualifiedNames.addAll(entityModel.getQualifiedNamesForSerializers());

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
