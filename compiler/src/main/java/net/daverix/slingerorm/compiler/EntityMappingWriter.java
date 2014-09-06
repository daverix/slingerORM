/*
 * Copyright 2014 David Laurell
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

import net.daverix.slingerorm.DatabaseConnection;
import net.daverix.slingerorm.Storage;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.exception.StorageException;
import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.ResultRow;
import net.daverix.slingerorm.mapping.ResultRows;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

public class EntityMappingWriter {
    public EntityMappingWriter() {

    }

    public void writeMapper(Writer bw, TypeElement entity, ProcessingEnvironment processingEnvironment) throws IOException {
        final TypeElementConverter typeElementConverter = new ProcessingTypeElementConverter(processingEnvironment);
        final EntityType entityType = new EntityType(entity, typeElementConverter);

        // -- Get values to append -------------------------------------------------------------------------------------
        final String mapperClassName = entityType.getMapperTypeName();
        final String entityName = entityType.getName();
        final String serializerTypeName = entityType.getSerializerTypeName();
        final String fullSerializerTypeName = entityType.getSerializerQualifiedName();
        final String idGetterName = entityType.getIdGetter();
        final String idFieldName = entityType.getPrimaryKeyDbName();

        final Map<String,String> fieldMethodGetterParts = entityType.getFieldMethodGetter();
        final List<String> setterMethodParts = entityType.getSetterMethodParts();

        final String tableName = entityType.getTableName();
        final String createTableSql = entityType.createTableSql();

        // -- Append the values ----------------------------------------------------------------------------------------
        appendHeader(bw, entity, entityName, fullSerializerTypeName);
        appendSerialierField(bw, serializerTypeName);
        appendConstructor(bw, mapperClassName, serializerTypeName);
        appendCreateTable(bw, createTableSql);
        appendInsert(bw, entityName, tableName, fieldMethodGetterParts);
        appendReplace(bw, entityName, tableName, fieldMethodGetterParts);
        appendUpdate(bw, entityName, tableName, idFieldName, idGetterName, fieldMethodGetterParts);
        appendDelete(bw, entityName, tableName, idFieldName, idGetterName);
        appendQuery(bw, entityName, tableName);
        appendQuerySingle(bw, entityName, tableName, idFieldName);
        appendMap(bw, entityName, setterMethodParts);
        appendFooter(bw);
    }

    // Append stuff ====================================================================================================

    protected void appendHeader(Writer bw, TypeElement entity, String entityName, String serializerName) throws IOException {
        final PackageElement packageElement = (PackageElement) entity.getEnclosingElement();

        bw.append("package ").append(packageElement.getQualifiedName()).append(";\n\n")
                .append("import ").append(Collection.class.getName()).append(";\n\n")
                .append("import ").append(List.class.getName()).append(";\n\n")
                .append("import ").append(ArrayList.class.getName()).append(";\n\n")
                .append("import ").append(FieldNotFoundException.class.getName()).append(";\n")
                .append("import ").append(ResultRow.class.getName()).append(";\n")
                .append("import ").append(ResultRows.class.getName()).append(";\n")
                .append("import ").append(InsertableValues.class.getName()).append(";\n")
                .append("import ").append(Storage.class.getName()).append(";\n")
                .append("import ").append(StorageException.class.getName()).append(";\n")
                .append("import ").append(DatabaseConnection.class.getName()).append(";\n")
                .append("import ").append(entity.getQualifiedName()).append(";\n\n")
                .append("import ").append(serializerName).append(";\n\n")
                .append("public class ").append(entityName).append("Storage implements Storage<").append(entityName).append("> {\n\n");
    }

    protected void appendSerialierField(Writer bw, String typeName) throws IOException {
        bw.append("    private ").append(typeName).append(" mSerializer;\n\n");
    }

    protected void appendConstructor(Writer bw, String mapperClassName, String serializerClassName) throws IOException {
        bw.append("    public ").append(mapperClassName).append("() {\n")
                .append("        mSerializer = new ").append(serializerClassName).append("();\n")
                .append("    }\n\n");
    }

    protected void appendFooter(Writer bw) throws IOException {
        bw.append("}\n");
    }

    protected void appendCreateTable(Writer bw, String createTableSql) throws IOException {
        bw.append("    @Override\n")
          .append("    public void createTable(DatabaseConnection connection) throws StorageException {\n")
          .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n")
          .append("        try {\n")
          .append("            connection.execSql(\"").append(createTableSql).append("\");\n")
          .append("        } catch (Exception e) {\n")
          .append("            throw new StorageException(e);\n")
          .append("        }\n")
          .append("    }\n\n");
    }

    protected void appendInsert(Writer bw, String entityName, String tableName, Map<String,String> fieldGetMethodParts) throws IOException {
        bw.append("    @Override\n")
          .append("    public void insert(DatabaseConnection connection, ").append(entityName).append(" item) throws StorageException {\n")
          .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n")
          .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n\n")
          .append("        try {\n")
          .append("            InsertableValues values = connection.createValues();\n");

        appendMapValues(bw, fieldGetMethodParts);

        bw.append("            connection.insert(\"").append(tableName).append("\", values);\n")
          .append("        } catch (Exception e) {\n")
          .append("            throw new StorageException(e);\n")
          .append("        }\n")
          .append("    }\n\n");
    }

    protected void appendReplace(Writer bw, String entityName, String tableName, Map<String,String> fieldGetMethodParts) throws IOException {
        bw.append("    @Override\n")
                .append("    public void replace(DatabaseConnection connection, ").append(entityName).append(" item) throws StorageException {\n")
                .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n")
                .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n\n")
                .append("        try {\n")
                .append("            InsertableValues values = connection.createValues();\n");

        appendMapValues(bw, fieldGetMethodParts);

        bw.append("            connection.replace(\"").append(tableName).append("\", values);\n")
                .append("        } catch (Exception e) {\n")
                .append("            throw new StorageException(e);\n")
                .append("        }\n")
                .append("    }\n\n");
    }


    protected void appendUpdate(Writer bw, String entityName, String tableName, String idFieldName, String idGetterName, Map<String,String> fieldGetMethodParts) throws IOException {
        bw.append("    @Override\n")
                .append("    public void update(DatabaseConnection connection, ").append(entityName).append(" item) throws StorageException {\n")
                .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n")
                .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n\n")
                .append("        try {\n")
                .append("            InsertableValues values = connection.createValues();\n");

        appendMapValues(bw, fieldGetMethodParts);

        bw.append("            connection.update(\"").append(tableName).append("\", values, \"").append(idFieldName).append("=?\", new String[]{String.valueOf(").append(idGetterName).append(")});\n")
                .append("        } catch (Exception e) {\n")
                .append("            throw new StorageException(e);\n")
                .append("        }\n")
                .append("    }\n\n");
    }

    protected void appendDelete(Writer bw, String entityName, String tableName, String idFieldName, String idGetterName) throws IOException {
        bw.append("    @Override\n")
                .append("    public void delete(DatabaseConnection connection, ").append(entityName).append(" item) throws StorageException {\n")
                .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n")
                .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n\n")
                .append("        try {\n")
                .append("            connection.delete(\"").append(tableName).append("\", \"").append(idFieldName).append("=?\", new String[]{String.valueOf(").append(idGetterName).append(")});\n")
                .append("        } catch (Exception e) {\n")
                .append("            throw new StorageException(e);\n")
                .append("        }\n")
                .append("    }\n\n");
    }

    protected void appendQuery(Writer bw, String entityName, String tableName) throws IOException {
        bw.append("    @Override\n")
                .append("    public Collection<").append(entityName).append("> query(DatabaseConnection connection, String selection, String[] selectionArgs, String orderBy) throws StorageException {\n")
                .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n\n")
                .append("        List<").append(entityName).append("> items = new ArrayList<").append(entityName).append(">();\n")
                .append("        try {\n")
                .append("            ResultRows result = null;")
                .append("            try {\n")
                .append("                result = connection.query(false, \"").append(tableName).append("\", null, selection, selectionArgs, null, null, orderBy);\n")
                .append("                for(ResultRow values : result) {\n")
                .append("                    items.add(map(values));\n")
                .append("                }\n")
                .append("            } finally {\n")
                .append("                if(result != null) {\n")
                .append("                    result.close();\n")
                .append("                }\n")
                .append("            }\n")
                .append("            return items;\n")
                .append("        } catch (Exception e) {\n")
                .append("            throw new StorageException(e);\n")
                .append("        }\n")
                .append("    }\n\n");
    }

    protected void appendQuerySingle(Writer bw, String entityName, String tableName, String idFieldName) throws IOException {
        bw.append("    @Override\n")
          .append("    public ").append(entityName).append(" querySingle(DatabaseConnection connection, String... ids) throws StorageException {\n")
          .append("        if(connection == null) throw new IllegalArgumentException(\"connection is null\");\n\n")
          .append("        if(ids == null) throw new IllegalArgumentException(\"ids is null\");\n\n")
          .append("        String id = ids[0];\n")
          .append("        try {\n")
          .append("            ResultRows result = null;\n")
          .append("            try {\n")
          .append("                result = connection.query(false, \"").append(tableName).append("\", null, \"").append(idFieldName).append("=?\", new String[]{id}, null, null, null);\n")
          .append("\n")
          .append("                if(result.iterator().hasNext()) {\n")
          .append("                    return map(result.iterator().next());\n")
          .append("                }\n")
          .append("                else {\n")
          .append("                    return null;\n")
          .append("                }\n")
          .append("            } finally {\n")
          .append("                if(result != null) {\n")
          .append("                    result.close();\n")
          .append("                }\n")
          .append("            }\n")
          .append("        } catch (Exception e) {\n")
          .append("            throw new StorageException(e);\n")
          .append("        }\n")
          .append("    }\n\n");
    }

    protected void appendMapValues(Writer bw, Map<String, String> fieldGetMethodParts) throws IOException {
        for(String fieldName : fieldGetMethodParts.keySet()) {
            final String getter = fieldGetMethodParts.get(fieldName);
            bw.append("            values.put(\"").append(fieldName).append("\", ").append(getter).append(");\n");
        }
    }

    protected void appendMap(Writer bw, String entityName, List<String> setters) throws IOException {
        bw.append("    private ").append(entityName).append(" map(ResultRow values) throws FieldNotFoundException {\n")
          .append("        ").append(entityName).append(" item = new ").append(entityName).append("();\n");
        for(String setter : setters) {
            bw.append("        item.").append(setter).append(";\n");
        }
        bw.append("        return item;\n")
                .append("    }\n\n");
    }
}
