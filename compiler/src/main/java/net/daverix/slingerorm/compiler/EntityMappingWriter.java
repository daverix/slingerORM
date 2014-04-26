package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.Mapping;
import net.daverix.slingerorm.mapping.ResultRow;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

public class EntityMappingWriter {

    private static final String RESULT_ROW_TYPE_NAME = ResultRow.class.getSimpleName();
    private static final String INSERTABLE_VALUES_TYPE_NAME = InsertableValues.class.getSimpleName();

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
        appendMapValues(bw, entityName, fieldMethodGetterParts);
        appendMap(bw, entityName, setterMethodParts);
        appendCreateTableSql(bw, createTableSql);
        appendGetTableName(bw, tableName);
        appendGetId(bw, entityName, idGetterName);
        appendGetIdFieldName(bw, idFieldName);
        appendFooter(bw);
    }

    // Append stuff ====================================================================================================

    protected void appendHeader(Writer bw, TypeElement entity, String entityName, String serializerName) throws IOException {
        final PackageElement packageElement = (PackageElement) entity.getEnclosingElement();

        bw.append("package ").append(packageElement.getQualifiedName()).append(";\n\n")
                .append("import ").append(FieldNotFoundException.class.getName()).append(";\n")
                .append("import ").append(ResultRow.class.getName()).append(";\n")
                .append("import ").append(InsertableValues.class.getName()).append(";\n")
                .append("import ").append(Mapping.class.getName()).append(";\n")
                .append("import ").append(entity.getQualifiedName()).append(";\n\n")
                .append("import ").append(serializerName).append(";\n\n")
                .append("public class ").append(entityName).append("Mapping implements Mapping<").append(entityName).append("> {\n\n");
    }

    protected void appendSerialierField(Writer bw, String typeName) throws IOException {
        bw.append("    private ").append(typeName).append(" mSerializer;\n\n");
    }

    protected void appendConstructor(Writer bw, String mapperClassName, String serializerClassName) throws IOException {
        bw.append("    public ").append(mapperClassName).append("(").append(serializerClassName).append(" serializer) {\n")
                .append("        mSerializer = serializer;\n")
                .append("    }\n\n");
    }

    protected void appendFooter(Writer bw) throws IOException {
        bw.append("}\n");
    }

    protected void appendMapValues(Writer bw, String entityName, Map<String,String> fieldGetMethodParts) throws IOException {
        bw.append("    @Override\n")
                .append("    public void mapValues(").append(entityName).append(" item, ").append(INSERTABLE_VALUES_TYPE_NAME).append(" values) {\n")
                .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n")
                .append("        if(values == null) throw new IllegalArgumentException(\"values is null\");\n\n");

        for(String fieldName : fieldGetMethodParts.keySet()) {
            final String getter = fieldGetMethodParts.get(fieldName);
            bw.append("        values.put(\"").append(fieldName).append("\", ").append(getter).append(");\n");
        }

        bw.append("    }\n\n");
    }

    protected void appendMap(Writer bw, String entityName, List<String> setters) throws IOException {
        bw.append("    @Override\n")
                .append("    public ").append(entityName).append(" map(").append(RESULT_ROW_TYPE_NAME).append(" values) throws FieldNotFoundException {\n")
                .append("        ").append(entityName).append(" item = new ").append(entityName).append("();\n");
        for(String setter : setters) {
            bw.append("        item.").append(setter).append(";\n");
        }
        bw.append("        return item;\n")
                .append("    }\n\n");
    }

    protected void appendCreateTableSql(Writer bw, String sql) throws IOException {
        bw.append("    @Override\n")
                .append("    public String getCreateTableSql() {\n")
                .append("        return \"").append(sql).append("\";\n")
                .append("    }\n\n");
    }

    protected void appendGetTableName(Writer bw, String tableName) throws IOException {
        bw.append("    @Override\n")
                .append("    public String getTableName() {\n")
                .append("        return \"").append(tableName).append("\";\n")
                .append("    }\n\n");
    }

    protected void appendGetId(Writer bw, String entityName, String idGetterName) throws IOException {
        bw.append("    @Override\n")
                .append("    public String getId(").append(entityName).append(" item) {\n")
                .append("        return String.valueOf(").append(idGetterName).append(");\n")
                .append("    }\n\n");
    }

    protected void appendGetIdFieldName(Writer bw, String idFieldName) throws IOException {
        bw.append("    @Override\n")
                .append("    public String getIdFieldName() {\n")
                .append("        return \"").append(idFieldName).append("\";\n")
                .append("    }\n");
    }
}
