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
        writer.write("        return new String[] { " + String.join(", ", getCitedColumnNames(entityModel)) + " };\n");
        writer.write("    }\n");
        writeln();

        writer.write("    @Override\n");
        writer.write("    public ContentValues mapValues(" + entityModel.getDatabaseEntityClassName() + " item) {\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n");
        writeln();
        writer.write("        ContentValues values = new ContentValues();\n");
        for(String field : fields) {
            String columnName = entityModel.getColumnName(field);
            writeValuesForField(columnName, "item", entityModel, field);
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
            String columnName = entityModel.getColumnName(field);
            writeSetterForField(entityModel, "item", field, columnName);
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
        writer.write("        return new String[]{" + getItemSqlArgumentsText(entityModel) + "};\n");
        writer.write("    }\n");
        writeln();
    }

    private String getItemSqlArgumentsText(DatabaseEntityModel entityModel) {
        List<String> getters = new ArrayList<String>();
        for(String field : entityModel.getItemSqlArgFields()) {
            if(entityModel.isFieldString(field)) {
                getters.add("item." + getEntityFieldMethod(entityModel, field));
            }
            else {
                getters.add("String.valueOf(item." + getEntityFieldMethod(entityModel, field) + ")");
            }
        }
        return String.join(", ", getters);
    }

    private void writeValuesForField(String columnName, String prefix, DatabaseEntityModel entityModel, String field) throws IOException {
        if (entityModel.isDatabaseEntity(field)) {
            DatabaseEntityModel fieldModel = entityModel.getDatabaseEntity(field);
            String primaryKey = new ArrayList<>(fieldModel.getPrimaryKeyFields()).get(0);
            writeValues(columnName, getValuesForFieldValue(prefix + "." + getEntityFieldMethod(entityModel, field), fieldModel, primaryKey));
            return;
        }

        writeValues(columnName, getValuesForFieldValue(prefix, entityModel, field));
    }

    private String getValuesForFieldValue(String prefix, DatabaseEntityModel entityModel, String field) {
        if(entityModel.hasSerializer(field)) {
            String serializerFieldName = entityModel.getSerializerFieldName(field);
            return serializerFieldName + ".serialize(" + prefix + "." + getEntityFieldMethod(entityModel, field) + ")";
        }
        else {
            return prefix + "." + getEntityFieldMethod(entityModel, field);
        }
    }

    private String getEntityFieldMethod(DatabaseEntityModel entityModel, String field) {
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

    private void writeSetterForField(DatabaseEntityModel entityModel, String variable, String field, String columnName) throws IOException {
        if(entityModel.hasSerializer(field)) {
            String serializerField = entityModel.getSerializerFieldName(field);
            CursorType cursorType = entityModel.getCursorType(field);
            writeMethod(variable, getSetter(entityModel, field, serializerField + ".deserialize(" + getCursorMethod(columnName, cursorType)) + ")");
        }
        else if(entityModel.isDatabaseEntity(field)) {
            writeSetterForDatabaseEntityField(entityModel, variable, field, columnName);
        }
        else {
            CursorType cursorType = entityModel.getCursorType(field);
            writeMethod(variable, getSetter(entityModel, field, getCursorMethod(columnName, cursorType)));
        }
    }

    private void writeSetterForDatabaseEntityField(DatabaseEntityModel entityModel, String variable, String field, String columnName) throws IOException {
        DatabaseEntityModel fieldModel = entityModel.getDatabaseEntity(field);
        writer.write("        " + fieldModel.getDatabaseEntityClassName() + " " + field + " = new " + fieldModel.getDatabaseEntityClassName() + "();\n");

        Set<String> subFields = fieldModel.getFieldNames();
        for(String subField : subFields) {
            String subColumnName = fieldModel.getColumnName(subField);
            writeSetterForField(fieldModel, field, subField, columnName + "_" + subColumnName);
        }

        writeMethod(variable, getSetter(entityModel, field, field));
    }

    private void writeMethod(String variable, String method) throws IOException {
        writer.write("        " + variable + "." + method + ";\n");
    }

    private String getSetter(DatabaseEntityModel entityModel, String field, String cursorValue) {
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

    private String getCursorMethod(String columnName, CursorType cursorType) {
        switch (cursorType) {
            case BYTE_ARRAY:
                return "cursor.getBlob(" + getColumnIndex(columnName) + ")";
            case SHORT:
                return "cursor.getShort(" + getColumnIndex(columnName) + ")";
            case INT:
                return "cursor.getInt(" + getColumnIndex(columnName) + ")";
            case LONG:
                return "cursor.getLong(" + getColumnIndex(columnName) + ")";
            case FLOAT:
                return "cursor.getFloat(" + getColumnIndex(columnName) + ")";
            case DOUBLE:
                return "cursor.getDouble(" + getColumnIndex(columnName) + ")";
            case STRING:
                return "cursor.getString(" + getColumnIndex(columnName) + ")";
            case BOOLEAN:
                return "cursor.getShort(" + getColumnIndex(columnName) + ") == 1";
            default:
                throw new IllegalStateException("unknown type");
        }
    }

    private String getColumnIndex(String columnName) {
        return "cursor.getColumnIndex(\"" + columnName + "\")";
    }

    private List<String> getCitedColumnNames(DatabaseEntityModel entityModel) {
        Collection<String> columnNames = entityModel.getColumnNames();
        List<String> cited = new ArrayList<String>();
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
        Set<String> qualifiedNames = new HashSet<String>();
        qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
        qualifiedNames.add("net.daverix.slingerorm.internal.EntityMapper");
        qualifiedNames.add("android.content.ContentValues");
        qualifiedNames.add("android.database.Cursor");
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
