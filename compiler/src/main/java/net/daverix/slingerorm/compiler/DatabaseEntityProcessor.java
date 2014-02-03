package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.annotation.*;
import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.IFetchableValues;
import net.daverix.slingerorm.mapping.IInsertableValues;
import net.daverix.slingerorm.mapping.IMapping;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class DatabaseEntityProcessor extends AbstractProcessor {
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_BIG_DECIMAL = "java.math.BigDecimal";
    private static final String TYPE_DATE = "java.util.Date";

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseEntity.class)) {
            try {
                if(isAbstract(entity)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Skipping abstract entity " + entity.getSimpleName());
                    continue;
                }

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating " + entity.getSimpleName() + "Mapping");
                createMapper((TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapping class: " + e.getLocalizedMessage());
            }
        }
        return true; // no further processing of this annotation type
    }

    protected void createMapper(TypeElement entity) throws IOException {
        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(entity.getQualifiedName() + "Mapping");
            BufferedWriter bw = new BufferedWriter(jfo.openWriter());

        writeMapper(bw, entity);
        bw.close();
    }

    protected void writeMapper(BufferedWriter bw, TypeElement entity) throws IOException {
        final String entityName = entity.getSimpleName().toString();
        final String tableName = getTableName(entity);
        final List<Element> validFields = getValidFields(entity);
        final List<Element> methods = getValidMethods(entity);
        final Element primaryKeyField = getPrimaryKeyField(entity, methods, validFields);

        appendHeader(bw, entity, entityName);
        appendMapValues(bw, methods, entity, entityName, validFields);
        appendMap(bw, entity, methods, entityName, validFields);
        appendCreateTableSql(bw, entity, tableName, validFields);
        appendGetTableName(bw, tableName);
        appendGetId(bw, entity, entityName, methods, primaryKeyField);
        appendGetIdFieldName(bw, primaryKeyField);
        appendFooter(bw);
    }

    protected void appendHeader(BufferedWriter bw, TypeElement entity, String entityName) throws IOException {
        final PackageElement packageElement = (PackageElement) entity.getEnclosingElement();

          bw.append("package ").append(packageElement.getQualifiedName()).append(";\n\n")
            .append("import ").append(FieldNotFoundException.class.getName()).append(";\n")
            .append("import ").append(IFetchableValues.class.getName()).append(";\n")
            .append("import ").append(IInsertableValues.class.getName()).append(";\n")
            .append("import ").append(IMapping.class.getName()).append(";\n")
            .append("import ").append(entity.getQualifiedName()).append(";\n\n")
            .append("public class ").append(entityName).append("Mapping")
            .append(" implements IMapping<").append(entityName).append("> {\n\n");
    }

    protected void appendFooter(BufferedWriter bw) throws IOException {
        bw.append("}\n");
    }

    protected void appendMapValues(BufferedWriter bw, List<Element> methods, Element entity, String entityName,
                                   List<Element> validFields) throws IOException {
          bw.append("    @Override\n")
            .append("    public void mapValues(").append(entityName).append(" item, IInsertableValues values) {\n")
            .append("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n")
            .append("        if(values == null) throw new IllegalArgumentException(\"values is null\");\n\n");

        for(Element field : validFields) {
            final String fieldName = getDatabaseFieldName(field);
            final String getter = findGetter(entity, methods, field);
          bw.append("        values.put(\"").append(fieldName).append("\", item.").append(getter).append(");\n");
        }

          bw.append("    }\n\n");
    }

    protected void appendMap(BufferedWriter bw, Element entity, List<Element> methods, String entityName, List<Element> validFields) throws IOException {
          bw.append("    @Override\n")
            .append("    public ").append(entityName).append(" map(IFetchableValues values) throws FieldNotFoundException {\n")
            .append("        ").append(entityName).append(" item = new ").append(entityName).append("();\n");
        for(Element field : validFields) {
          bw.append("        item.").append(getSetterWithParam(entity, methods, field, getValuePart(field))).append(";\n");
        }
          bw.append("        return item;\n")
            .append("    }\n\n");
    }

    protected void appendCreateTableSql(BufferedWriter bw, Element entity, String tableName, List<Element> validFields) throws IOException {
        final String createTableSql = createTableSql(entity, tableName, validFields);

          bw.append("    @Override\n")
            .append("    public String getCreateTableSql() {\n")
            .append("        return \"").append(createTableSql).append("\";\n")
            .append("    }\n\n");
    }

    protected void appendGetTableName(BufferedWriter bw, String tableName) throws IOException {
          bw.append("    @Override\n")
            .append("    public String getTableName() {\n")
            .append("        return \"").append(tableName).append("\";\n")
            .append("    }\n\n");
    }

    protected void appendGetId(BufferedWriter bw, TypeElement entity, String entityName, List<Element> methods, Element primaryKeyField) throws IOException {
        final String idGetterName = getIdGetter(entity, methods, primaryKeyField);
          bw.append("    @Override\n")
            .append("    public String getId(").append(entityName).append(" item) {\n")
            .append("        return String.valueOf(item.").append(idGetterName).append(");\n")
            .append("    }\n\n");
    }

    protected void appendGetIdFieldName(BufferedWriter bw, Element primaryKeyField) throws IOException {
        final String idFieldName = getIdFieldname(primaryKeyField);
          bw.append("    @Override\n")
            .append("    public String getIdFieldName() {\n")
            .append("        return \"").append(idFieldName).append("\";\n")
            .append("    }\n");
    }

    protected String createTableSql(Element entity, String tableName, List<Element> validFields) {
        if(validFields.size() == 0)
            throw new IllegalArgumentException("no fields found in " + entity.getSimpleName());

        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(");

        for(int i=0;i<validFields.size();i++) {
            final Element field = validFields.get(i);
            final String fieldName = getDatabaseFieldName(field);
            final String fieldType = getDatabaseType(field);
            builder.append(fieldName).append(" ").append(fieldType);
            PrimaryKey annotation = field.getAnnotation(PrimaryKey.class);
            if(annotation != null) {
                builder.append(" NOT NULL PRIMARY KEY");
            }

            if(i < validFields.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(")");

        return builder.toString();
    }

    protected String getDatabaseType(Element field) {
        TypeMirror fieldType = field.asType();
        TypeKind typeKind = fieldType.getKind();
        switch (typeKind) {
            case BOOLEAN:
            case SHORT:
            case LONG:
            case INT:
                return "INTEGER";
            case FLOAT:
            case DOUBLE:
                return "REAL";
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) fieldType;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                String typeName = typeElement.getQualifiedName().toString();
                if(typeName.equals(TYPE_DATE)) {
                    return "INTEGER";
                } else if(typeName.equals(TYPE_BIG_DECIMAL)) {
                    return "REAL";
                } else if(typeName.equals(TYPE_STRING)) {
                    return "TEXT";
                } else {
                    throw new UnsupportedOperationException(typeName + " not supported by SlingerORM");
                }
            default:
                throw new UnsupportedOperationException(field.getSimpleName() + " have a type not known by SlingerORM");
        }
    }

    protected List<Element> getValidMethods(TypeElement entity) {
        List<Element> methods = new ArrayList<Element>();
        addElements(methods, entity, new Verifier<Element>() {
            @Override
            public boolean verify(Element item) {
                return item.getKind() == ElementKind.METHOD && isAccessable(item);
            }
        });
        return methods;
    }
     protected List<Element> getValidFields(TypeElement entity) {
        List<Element> fields = new ArrayList<Element>();
        addElements(fields, entity, new Verifier<Element>() {
            @Override
            public boolean verify(Element item) {
                return item.getKind() == ElementKind.FIELD && isDatabaseField(item);
            }
        });
        return fields;
    }

    protected void addElements(List<Element> fields, TypeElement entity, Verifier<Element> verifier) {
        for(Element element: entity.getEnclosedElements()) {
            if(verifier.verify(element)) {
                fields.add(element);
            }
        }

        TypeMirror parentMirror = entity.getSuperclass();
        if(parentMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) parentMirror;
            addElements(fields, (TypeElement) declaredType.asElement(), verifier);
        }
    }

    protected String getValuePart(Element field) {
        TypeMirror fieldType = field.asType();
        String fieldName = getDatabaseFieldName(field);
        TypeKind typeKind = fieldType.getKind();

        switch (typeKind) {
            case BOOLEAN:
                return "values.getBoolean(\"" + fieldName + "\")";
            case SHORT:
                return "values.getShort(\"" + fieldName + "\")";
            case INT:
                return "values.getInt(\"" + fieldName + "\")";
            case LONG:
                return "values.getLong(\"" + fieldName + "\")";
            case FLOAT:
                return "values.getFloat(\"" + fieldName + "\")";
            case DOUBLE:
                return "values.getDouble(\"" + fieldName + "\")";
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) fieldType;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                String typeName = typeElement.getQualifiedName().toString();
                if(typeName.equals(TYPE_DATE)) {
                    return "values.getDate(\"" + fieldName + "\")";
                } else if(typeName.equals(TYPE_BIG_DECIMAL)) {
                    return "values.getBigDecimal(\"" + fieldName + "\")";
                } else if(typeName.equals(TYPE_STRING)) {
                    return "values.getString(\"" + fieldName + "\")";
                }

                throw new UnsupportedOperationException(typeName + " is not supported by SlingerORM");
            default:
                throw new UnsupportedOperationException(fieldName + " have a type not known by SlingerORM");
        }
    }

    protected String getSetterWithParam(Element entity, List<Element> methods, Element field, String valuePart) {
        Element method = findMethodByFieldNameAndSetFieldAnnotation(methods, field.getSimpleName().toString());
        if(method != null)
            return method.getSimpleName() + "(" + valuePart + ")";

        method = findMethodByFieldNameOnly(methods, field.getSimpleName().toString(), "set");
        if(method != null)
            return method.getSimpleName() + "(" + valuePart + ")";

        if (!isAccessable(field))
            throw new IllegalStateException("No get method or a public field for " + field.getSimpleName() + " in " + entity.getSimpleName());

        return field.getSimpleName().toString() + "=" + valuePart;
    }

    protected Element findMethodByFieldNameAndSetFieldAnnotation(List<? extends Element> elements, String fieldName) {
        for(Element element : elements) {
            SetField annotation = element.getAnnotation(SetField.class);
            if(annotation == null)
                continue;

            String fieldReference = annotation.value();
            if(fieldReference == null || fieldReference.equals(""))
                throw new IllegalStateException(element.getSimpleName() + " has a SetField annotation with empty value!");

            if(fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    protected String getIdFieldname(Element primaryKeyField) {
        return getDatabaseFieldName(primaryKeyField);
    }

    protected String getIdGetter(TypeElement entity, List<Element> methods, Element primaryKeyField) {
        return findGetter(entity, methods, primaryKeyField);
    }

    protected Element getPrimaryKeyField(TypeElement entity, List<Element> methods, List<Element> validFields) {
        Element field = findSingleElementByAnnotation(validFields, PrimaryKey.class);
        if (field == null)
            field = getPrimaryKeyFieldUsingDatabaseEntity(entity, validFields);

        if(field == null)
            throw new IllegalStateException("There must be a field annotated with PrimaryKey or the key specified in @DatabaseEntity is empty!");

        return field;
    }

    protected Element getPrimaryKeyFieldUsingDatabaseEntity(TypeElement entity, List<Element> validFields) {
        DatabaseEntity annotation = entity.getAnnotation(DatabaseEntity.class);
        String key = annotation.primaryKey();
        if(key == null || key.equals(""))
            return null;

        Element field = getFieldByName(validFields, key);
        if(field == null)
            throw new IllegalStateException("Field specified in DatabaseEntity doesn't exist in entity class!");

        return field;
    }

    protected Element getFieldByName(List<Element> fields, String name) {
        for(Element field : fields) {
            if(name.equals(field.getSimpleName().toString())) {
                return field;
            }
        }

        return null;
    }

    protected Element findSingleElementByAnnotation(List<? extends Element> elements, Class<? extends Annotation> annotationClass) {
        for(Element element : elements) {
            Annotation annotation = element.getAnnotation(annotationClass);

            if(annotation != null)
                return element;
        }

        return null;
    }

    protected String getTableName(Element entity) {
        DatabaseEntity annotation = entity.getAnnotation(DatabaseEntity.class);
        String tableName = annotation.name();
        if(tableName == null || tableName.equals(""))
            return entity.getSimpleName().toString();

        return tableName;
    }

    protected String getDatabaseFieldName(Element field) {
        FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
        if(fieldNameAnnotation == null)
            return field.getSimpleName().toString();

        String fieldName = fieldNameAnnotation.value();
        if(fieldName == null || fieldName.equals(""))
            throw new IllegalStateException("fieldName must not be null or empty!");

        return fieldName;
    }

    protected String findGetter(Element entity, List<Element> methods, Element field) {
        Element method = findMethodByFieldNameAndGetFieldAnnotation(methods, field.getSimpleName().toString());
        if(method != null)
            return method.getSimpleName() + "()";

        method = findMethodByFieldNameOnly(methods, field.getSimpleName().toString(), "get");
        if(method != null)
            return method.getSimpleName() + "()";

        if (!isAccessable(field))
            throw new IllegalStateException("No get method or a public field for " + field.getSimpleName() + " in " + entity.getSimpleName());

        return field.getSimpleName().toString();
    }

    protected Element findMethodByFieldNameOnly(List<? extends Element> elements, String fieldName, String prefix) {
        for(Element element : elements) {
            if(element.getKind() == ElementKind.METHOD) {
                String firstLetter = fieldName.substring(0, 1).toUpperCase();
                String methodName = prefix + firstLetter + fieldName.substring(1);
                
                if(methodName.equals(element.getSimpleName().toString()))
                    return element;
            }
        }

        return null;
    }

    protected Element findMethodByFieldNameAndGetFieldAnnotation(List<? extends Element> elements, String fieldName) {
        for(Element element : elements) {
            GetField annotation = element.getAnnotation(GetField.class);
            if(annotation == null)
                continue;

            String fieldReference = annotation.value();
            if(fieldReference == null || fieldReference.equals(""))
                throw new IllegalStateException(element.getSimpleName() + " has a GetField annotation with empty value!");

            if(fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    protected boolean isAbstract(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("ABSTRACT".equals(name)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isDatabaseField(Element field) {
        Set<Modifier> modifiers = field.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("STATIC".equals(name) || "TRANSIENT".equals(name)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isAccessable(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("PRIVATE".equals(name) ||
                    "PROTECTED".equals(name) ||
                    "STATIC".equals(name) ||
                    "TRANSIENT".equals(name)) {
                return false;
            }
        }

        return true;
    }

    private interface Verifier<T> {
        public boolean verify(T item);
    }
}
