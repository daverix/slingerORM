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

import com.google.auto.service.AutoService;

import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.DeserializeType;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.NotDatabaseField;
import net.daverix.slingerorm.annotation.PrimaryKey;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.SerializeType;
import net.daverix.slingerorm.annotation.Update;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static net.daverix.slingerorm.compiler.ElementUtils.getElementsInTypeElement;
import static net.daverix.slingerorm.compiler.ListUtils.filter;

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseStorage")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseStorageProcessor extends AbstractProcessor {
    private static final String QUALIFIED_NAME_SQLITE_DATABASE = "android.database.sqlite.SQLiteDatabase";
    private static final List<String> SUPPORTED_RETURN_TYPES_FOR_SELECT = Arrays.asList(
            "java.util.List",
            "java.util.Collection",
            "java.lang.Iterable"
    );

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseStorage.class)) {
            try {
                createStorage((TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.getLocalizedMessage());
            } catch (InvalidElementException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.getMessage(), e.getElement());
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Internal error: " + StacktraceUtils.getStackTraceString(e));
            }
        }
        return true; // no further processing of this annotation type
    }

    private void createStorage(TypeElement entity) throws IOException, InvalidElementException {
        if(entity == null) throw new IllegalArgumentException("entity is null");

        String qualifiedName = entity.getQualifiedName().toString();
        String packageName = getPackage(qualifiedName);
        String storageImplName = "Slinger_" + entity.getSimpleName();

        DatabaseStorage databaseStorage = entity.getAnnotation(DatabaseStorage.class);
        TypeElement serializerElement = getSerializerElement(databaseStorage);
        String serializerQualifiedName = serializerElement.getQualifiedName().toString();
        String serializerClassName = serializerElement.getSimpleName().toString();

        List<StorageMethod> methods = getStorageMethods(entity, serializerElement);

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName + "." + storageImplName);
        BufferedWriter bw = new BufferedWriter(jfo.openWriter());
        try {
            StorageClassBuilder.builder(bw)
                    .setPackage(packageName)
                    .setClassName(storageImplName)
                    .setStorageInterfaceName(entity.getSimpleName().toString())
                    .setSerializer(serializerQualifiedName, serializerClassName, hasEmptyConstructor(serializerElement))
                    .addMethods(methods)
                    .build();
        } finally {
            bw.close();
        }
    }

    private List<StorageMethod> getStorageMethods(TypeElement element, TypeElement serializerTypeElement) throws InvalidElementException {
        if(element == null) throw new IllegalArgumentException("element is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        List<StorageMethod> methods = new ArrayList<StorageMethod>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                methods.add(createStorageMethod((ExecutableElement) enclosedElement, serializerTypeElement));
            }
        }
        return methods;
    }

    private StorageMethod createStorageMethod(ExecutableElement methodElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        checkFirstParameterMustBeSQLiteDatabase(methodElement);

        if (isAnnotationPresent(methodElement, Insert.class)) {
            return createInsertMethod(methodElement, serializerTypeElement);
        } else if (isAnnotationPresent(methodElement, Replace.class)) {
            return createReplaceMethod(methodElement, serializerTypeElement);
        } else if (isAnnotationPresent(methodElement, Update.class)) {
            return createUpdateMethod(methodElement, serializerTypeElement);
        } else if (isAnnotationPresent(methodElement, Delete.class)) {
            return createDeleteMethod(methodElement);
        } else if (isAnnotationPresent(methodElement, Select.class)) {
            return createSelectMethod(methodElement);
        } else if(isAnnotationPresent(methodElement, CreateTable.class)) {
            return createCreateTableMethod(methodElement, serializerTypeElement);
        } else {
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must be annotated with either @CreateTable, @Insert, @Replace, @Update, @Delete or @Select", methodElement);
        }
    }

    private StorageMethod createCreateTableMethod(ExecutableElement methodElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        checkUniqueAnnotations(CreateTable.class, methodElement);
        checkHasVoidReturnType(methodElement);

        TypeElement databaseTypeElement = getCreateTableDatabaseEntity(methodElement.getAnnotation(CreateTable.class));
        return new CreateTableMethod(methodElement.getSimpleName().toString(),
                createTableSql(databaseTypeElement, serializerTypeElement));
    }

    private StorageMethod createSelectMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Select.class, methodElement);

        Select selectAnnotation = methodElement.getAnnotation(Select.class);
        String where = selectAnnotation.where();
        String orderBy = selectAnnotation.orderBy() == null || selectAnnotation.orderBy().equals("") ? null : selectAnnotation.orderBy();

        //TODO: check if where contains correct variables specified in databaseEntity

        TypeMirror returnType = methodElement.getReturnType();
        if(returnType.getKind() != TypeKind.DECLARED)
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", methodElement);

        TypeElement returnTypeElement = (TypeElement) ((DeclaredType) returnType).asElement();

        List<? extends VariableElement> parameters = methodElement.getParameters();
        Collection<String> parameterGetters = getParameterNames(parameters);
        String parameterText = getParameterText(parameters);

        if(returnTypeElement.getAnnotation(DatabaseEntity.class) != null) {
            return new SelectSingleMethod(methodElement.getSimpleName().toString(),
                    getTableName(returnTypeElement),
                    returnTypeElement.getSimpleName().toString(),
                    parameterText,
                    where,
                    parameterGetters);
        }
        else if(SUPPORTED_RETURN_TYPES_FOR_SELECT.contains(returnTypeElement.getQualifiedName().toString())) {
            TypeMirror typeMirror = ((DeclaredType) returnType).getTypeArguments().get(0);
            TypeElement databaseEntityElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
            String returnTypeName = databaseEntityElement.getSimpleName().toString();

            return new SelectMultipleMethod(methodElement.getSimpleName().toString(),
                    getTableName(databaseEntityElement),
                    returnTypeElement.getSimpleName() + "<" + returnTypeName + ">",
                    parameterText,
                    where,
                    parameterGetters,
                    orderBy);
        }
        else {
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", methodElement);
        }
    }

    private Collection<String> getParameterNames(List<? extends VariableElement> parameters) {
        if(parameters == null) throw new IllegalArgumentException("parameters is null");

        List<String> parameterNames = new ArrayList<String>();
        for(VariableElement variableElement : parameters) {
            parameterNames.add(variableElement.getSimpleName().toString());
        }
        return parameterNames;
    }

    private String getParameterText(List<? extends VariableElement> parameters) throws InvalidElementException {
        if(parameters == null) throw new IllegalArgumentException("parameters is null");

        StringBuilder builder = new StringBuilder();
        for(int i=0;i<parameters.size();i++) {
            VariableElement variableElement = parameters.get(i);
            String typeName = getTypeName(variableElement.asType().getKind(), variableElement);
            builder.append(typeName).append(" ").append(variableElement.getSimpleName());

            if(i < parameters.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private String getTypeName(TypeKind typeKind, Element element) throws InvalidElementException {
        if(typeKind == null) throw new IllegalArgumentException("typeKind is null");
        if(element == null) throw new IllegalArgumentException("element is null");

        switch (typeKind) {
            case INT:
                return "int";
            case SHORT:
                return "short";
            case LONG:
                return "long";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case CHAR:
                return "char";
            case BYTE:
                return "byte";
            case BOOLEAN:
                return "boolean";
            case DECLARED:
                TypeElement typeElement = (TypeElement) ((DeclaredType) element.asType()).asElement();
                return typeElement.getSimpleName().toString();
            default:
                throw new InvalidElementException(typeKind + " is not known, bug?", element);
        }
    }

    private StorageMethod createDeleteMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Delete.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);

        String where = getPrimaryKeyDbName(databaseEntityElement) + "=?";
        Element primaryKeyField = getPrimaryKeyField(databaseEntityElement);
        String directGetter = findDirectGetter(databaseEntityElement, primaryKeyField);
        List<String> args = Arrays.asList(directGetter);

        return new DeleteMethod(methodElement.getSimpleName().toString(),
                getTableName(databaseEntityElement),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                where, args);
    }

    private StorageMethod createUpdateMethod(ExecutableElement methodElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        checkUniqueAnnotations(Update.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);

        String where = getPrimaryKeyDbName(databaseEntityElement) + "=?";
        Element primaryKeyField = getPrimaryKeyField(databaseEntityElement);
        String directGetter = findDirectGetter(databaseEntityElement, primaryKeyField);
        List<String> args = Arrays.asList(directGetter);

        return new UpdateMethod(methodElement.getSimpleName().toString(),
                getTableName(databaseEntityElement),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                getFieldMethodGetter(databaseEntityElement, serializerTypeElement),
                where, args);
    }

    private StorageMethod createReplaceMethod(ExecutableElement methodElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        checkUniqueAnnotations(Replace.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);
        checkHasTwoParameters(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);

        return new ReplaceMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                getTableName(databaseEntityElement),
                getFieldMethodGetter(databaseEntityElement, serializerTypeElement));
    }

    private StorageMethod createInsertMethod(ExecutableElement methodElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        checkUniqueAnnotations(Insert.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);
        checkHasTwoParameters(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);

        return new InsertMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                getTableName(databaseEntityElement),
                getFieldMethodGetter(databaseEntityElement, serializerTypeElement));
    }

    private String getPrimaryKeyDbName(TypeElement databaseEntityElement) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");

        return getDatabaseFieldName(getPrimaryKeyField(databaseEntityElement));
    }

    private Element getPrimaryKeyField(TypeElement databaseEntityElement) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");

        List<Element> fields = getFieldsUsedInDatabase(databaseEntityElement);
        Element field = findSingleElementByAnnotation(fields, PrimaryKey.class);
        if (field == null)
            field = getPrimaryKeyFieldUsingDatabaseEntity(databaseEntityElement, fields);

        if(field == null)
            throw new InvalidElementException("There must be a field annotated with PrimaryKey or the key specified in @DatabaseEntity is empty!", databaseEntityElement);

        return field;
    }

    private Element findSingleElementByAnnotation(List<Element> elements, final Class<? extends Annotation> annotationClass) throws InvalidElementException {
        if(elements == null) throw new IllegalArgumentException("elements is null");
        if(annotationClass == null) throw new IllegalArgumentException("annotationClass");

        return ListUtils.firstOrDefault(filter(elements, new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getAnnotation(annotationClass) != null;
            }
        }));
    }

    protected Element getPrimaryKeyFieldUsingDatabaseEntity(TypeElement entity, List<Element> validFields) throws InvalidElementException {
        if(entity == null) throw new IllegalArgumentException("entity is null");
        if(validFields == null) throw new IllegalArgumentException("validFields");

        DatabaseEntity annotation = entity.getAnnotation(DatabaseEntity.class);
        String key = annotation.primaryKey();
        if(key == null || key.equals(""))
            return null;

        Element field = getFieldByName(validFields, key);
        if(field == null)
            throw new InvalidElementException("Field specified in DatabaseEntity annotation doesn't exist in entity class!", entity);

        return field;
    }

    protected Element getFieldByName(List<Element> fields, final String name) throws InvalidElementException {
        if(fields == null) throw new IllegalArgumentException("fields is null");
        if(name == null) throw new IllegalArgumentException("name is null");

        return ListUtils.firstOrDefault(filter(fields, new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return name.equals(item.getSimpleName().toString());
            }
        }));
    }

    public Map<String,String> getFieldMethodGetter(TypeElement databaseEntityElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        Map<String, String> fieldMethodGetterParts = new HashMap<String, String>();
        for(Element field : getFieldsUsedInDatabase(databaseEntityElement)) {
            final String fieldName = getDatabaseFieldName(field);
            final String getter = findGetter(databaseEntityElement, serializerTypeElement, field);
            fieldMethodGetterParts.put(fieldName, getter);
        }
        return fieldMethodGetterParts;
    }

    protected String findGetter(TypeElement databaseEntityElement, TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        final TypeKind fieldTypeKind = ElementUtils.getTypeKind(field);
        final ObjectType objectType = getObjectTypeForElement(fieldTypeKind, field);

        switch (objectType) {
            case BOOLEAN:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
            case STRING:
                return findDirectGetter(databaseEntityElement, field);
            case OTHER:
                return findGetterInSerializer(databaseEntityElement, serializerTypeElement, field);
            default:
                throw new UnsupportedOperationException("this should not be called!");
        }
    }

    private String findDirectGetter(TypeElement databaseEntityElement, Element field) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        List<ExecutableElement> methodsInDatabaseEntityElement = ElementUtils.getMethodsInTypeElement(databaseEntityElement);
        ExecutableElement method = findMethodByFieldNameAndGetFieldAnnotation(methodsInDatabaseEntityElement, field.getSimpleName().toString());
        if(method != null)
            return "entity." + method.getSimpleName() + "()";

        boolean isBoolean = field.asType().getKind() == TypeKind.BOOLEAN;
        method = findMethodByFieldNameOnly(methodsInDatabaseEntityElement, field.getSimpleName().toString(), isBoolean ? "is" : "get");
        if(method != null)
            return "entity." + method.getSimpleName() + "()";

        if (!ElementUtils.isAccessible(field))
            throw new InvalidElementException("No get method or a public field for " + field.getSimpleName() + " in " + databaseEntityElement.getSimpleName(),
                    databaseEntityElement);

        return "entity." + field.getSimpleName().toString();
    }

    private ExecutableElement findMethodByFieldNameOnly(List<ExecutableElement> methods, final String fieldName, final String prefix) throws InvalidElementException {
        if(methods == null) throw new IllegalArgumentException("methods is null");
        if(fieldName == null) throw new IllegalArgumentException("fieldName is null");
        if(prefix == null) throw new IllegalArgumentException("prefix is null");

        return ListUtils.firstOrDefault(filter(methods, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                String firstLetter = fieldName.substring(0, 1).toUpperCase();
                String methodName = prefix + firstLetter + fieldName.substring(1);

                return methodName.equals(item.getSimpleName().toString());
            }
        }));
    }

    private ExecutableElement findMethodByFieldNameAndGetFieldAnnotation(List<ExecutableElement> elements, String fieldName) throws InvalidElementException {
        if(elements == null) throw new IllegalArgumentException("elements is null");
        if(fieldName == null) throw new IllegalArgumentException("fieldName is null");

        for(ExecutableElement element : elements) {
            GetField annotation = element.getAnnotation(GetField.class);
            if(annotation == null)
                continue;

            String fieldReference = annotation.value();
            if(fieldReference == null || fieldReference.equals(""))
                throw new InvalidElementException(element.getSimpleName() + " has a GetField annotation with empty value!", element);

            if(fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    private String findGetterInSerializer(TypeElement databaseEntityElement, TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        final List<ExecutableElement> methodsInSerializer = getSerializeMethodsInSerializer(serializerTypeElement);
        final List<ExecutableElement> methods = getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(methodsInSerializer, field);

        if(methods.size() == 0) {
            throw new InvalidElementException("No method found for field " + field.asType(), field);
        }

        if(methods.size() > 1) {
            throw new InvalidElementException("Only one @SerializeType method per parameter type supported, found " + methods.size() + " methods with same parameter type in " + field.asType(),
                    field);
        }

        return "serializer." + methods.get(0).getSimpleName() + "(" + findDirectGetter(databaseEntityElement, field) + ")";
    }

    protected List<ExecutableElement> getSerializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return filter(getMethodsInSerializer(serializerTypeElement), new Predicate<ExecutableElement>() {
                @Override
                public boolean test(ExecutableElement item) {
                    return item.getAnnotation(SerializeType.class) != null;
                }
            });
    }

    protected List<ExecutableElement> getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(List<ExecutableElement> methods, final Element element) throws InvalidElementException {
        if(methods == null) throw new IllegalArgumentException("methods is null");
        if(element == null) throw new IllegalArgumentException("element is null");

        final TypeKind typeKind = ElementUtils.getTypeKind(element);
        final ObjectType objectType = getObjectTypeForElement(typeKind, element);
        if(objectType != ObjectType.OTHER)
            throw new InvalidElementException("Element should be a declared type", element);

        final DeclaredType parameterDeclaredType = (DeclaredType) element.asType();
        final TypeElement parameterTypeElement = (TypeElement) parameterDeclaredType.asElement();
        final String elementTypeName = parameterTypeElement.getQualifiedName().toString();

        return filter(methods, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) throws InvalidElementException {
                final List<? extends VariableElement> parameters = item.getParameters();
                if (parameters.size() != 1) {
                    throw new InvalidElementException("@DeserializeType/@SerializeType methods must have one parameter, got " + parameters.size() + " parameters for method " + item.getSimpleName(), item);
                }

                final VariableElement variableElement = parameters.get(0);
                final TypeKind parameterTypeKind = ElementUtils.getTypeKind(variableElement);

                if(!typeKind.equals(parameterTypeKind))
                    return false;

                ObjectType paramterObjectType = getObjectTypeForElement(parameterTypeKind, element);
                if(paramterObjectType != ObjectType.OTHER)
                    return false;

                final DeclaredType parameterDeclaredType = (DeclaredType) variableElement.asType();
                final TypeElement parameterTypeElement = (TypeElement) parameterDeclaredType.asElement();
                final String parameterTypeName = parameterTypeElement.getQualifiedName().toString();

                return parameterTypeName.equals(elementTypeName);
            }
        });
    }

    public ObjectType getObjectTypeForElement(TypeKind typeKind, Element element) throws InvalidElementException {
        if(typeKind == null) throw new IllegalArgumentException("typeKind is null");
        if(element == null) throw new IllegalArgumentException("element is null");

        switch (typeKind) {
            case BOOLEAN:
                return ObjectType.BOOLEAN;
            case SHORT:
                return ObjectType.SHORT;
            case INT:
                return ObjectType.INT;
            case LONG:
                return ObjectType.LONG;
            case FLOAT:
                return ObjectType.FLOAT;
            case DOUBLE:
                return ObjectType.DOUBLE;
            case DECLARED:
                final DeclaredType declaredType = (DeclaredType) element.asType();
                final TypeElement typeElement = (TypeElement) declaredType.asElement();
                final String typeName = typeElement.getQualifiedName().toString();
                if(typeName.equals(ElementUtils.TYPE_STRING)) {
                    return ObjectType.STRING;
                }
                else {
                    return ObjectType.OTHER;
                }
            default:
                throw new InvalidElementException(typeKind + " is not known by SlingerORM, solve this by creating a custom serializer method", element);
        }
    }

    public String createTableSql(TypeElement databaseTypeElement, TypeElement serializerTypeElement) throws InvalidElementException {
        if(databaseTypeElement == null) throw new IllegalArgumentException("databaseTypeElement is null");
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        final List<Element> fields = getFieldsUsedInDatabase(databaseTypeElement);
        if(getFieldsUsedInDatabase(databaseTypeElement).size() == 0)
            throw new InvalidElementException("no fields found in " + databaseTypeElement.getSimpleName(), databaseTypeElement);

        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableName(databaseTypeElement)).append("(");

        DatabaseEntity entityAnnotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String primaryKey = entityAnnotation.primaryKey();

        boolean primaryKeySet = false;

        for(int i=0;i<fields.size();i++) {
            final Element field = fields.get(i);
            final String fieldName = getDatabaseFieldName(field);
            final String fieldType = getDatabaseType(serializerTypeElement, field);
            builder.append(fieldName).append(" ").append(fieldType);
            PrimaryKey annotation = field.getAnnotation(PrimaryKey.class);
            if( annotation != null || ( primaryKey != null && !primaryKey.equals("") && primaryKey.equals(field.getSimpleName().toString()) )) {
                builder.append(" NOT NULL PRIMARY KEY");
                primaryKeySet = true;
            }

            if(i < fields.size() - 1) {
                builder.append(", ");
            }
        }

        if(!primaryKeySet)
            throw new InvalidElementException("Primary key not found when creating SQL for entity " + databaseTypeElement.getSimpleName(), databaseTypeElement);

        builder.append(")");

        return builder.toString();
    }

    protected String getDatabaseType(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        TypeMirror fieldType = field.asType();
        TypeKind typeKind = fieldType.getKind();
        switch (typeKind) {
            case BOOLEAN:
            case SHORT:
            case LONG:
            case INT:
            case FLOAT:
            case DOUBLE:
                return getNativeTypeForDatabase(field);
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) fieldType;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                String typeName = typeElement.getQualifiedName().toString();

                if(typeName.equals(ElementUtils.TYPE_STRING)) {
                    return getNativeTypeForDatabase(field);
                }
                else {
                    return getDatabaseTypeFromMethodParameterInSerializer(getDeserializeMethodsInSerializer(serializerTypeElement), field);
                }
            default:
                throw new InvalidElementException(field.getSimpleName() + " have a type not known by SlingerORM, solve this by creating a custom serializer", field);
        }
    }

    protected String getDatabaseTypeFromMethodParameterInSerializer(List<ExecutableElement> methods, Element field) throws InvalidElementException {
        if(methods == null) throw new IllegalArgumentException("methods is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        final ExecutableElement serializerMethod = getMethodInSerializerThatMatchesReturnTypeElement(methods, field);
        final TypeMirror typeMirror = serializerMethod.getReturnType();
        final TypeKind typeKind = typeMirror.getKind();
        if(typeKind != TypeKind.DECLARED) {
            throw new InvalidElementException("TypeKind should be declared but was " + typeKind, serializerMethod);
        }

        final DeclaredType declaredType = (DeclaredType) typeMirror;
        final TypeElement typeElement = (TypeElement) declaredType.asElement();
        final String typeName = typeElement.getQualifiedName().toString();
        if(typeName.equals(ElementUtils.TYPE_STRING)) {
            throw new InvalidElementException("String type should not be passed to this method", serializerMethod);
        }

        final List<? extends VariableElement> parameters = serializerMethod.getParameters();
        if(parameters.size() != 1) {
            throw new InvalidElementException("@DeserializeType/@SerializeType methods must have one parameter with a native type or String, got " + parameters.size() + " parameters for method " + serializerMethod.getSimpleName(), serializerMethod);
        }

        return getNativeTypeForDatabase(parameters.get(0));
    }

    protected List<ExecutableElement> getDeserializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return filter(getMethodsInSerializer(serializerTypeElement), new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                return item.getAnnotation(DeserializeType.class) != null;
            }
        });
    }

    protected List<ExecutableElement> getMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return ElementUtils.getMethodsInTypeElement(serializerTypeElement);
    }

    protected ExecutableElement getMethodInSerializerThatMatchesReturnTypeElement(List<ExecutableElement> methods, Element field) throws InvalidElementException {
        if(methods == null) throw new IllegalArgumentException("methods is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        final String typeName = ElementUtils.getDeclaredTypeName(field);
        final List<ExecutableElement> methodsWithCorrectReturnType = getMethodsWithReturnElement(methods, field);
        if(methodsWithCorrectReturnType.size() < 1) {
            throw new InvalidElementException("No @DeserializeType methods found with return type " + typeName, field);
        }

        if(methodsWithCorrectReturnType.size() > 1) {
            throw new InvalidElementException("Return value of @DeserializeType/@SerializeType methods must be unique, there are " + methodsWithCorrectReturnType.size() + " with return type " + typeName, field);
        }

        return methodsWithCorrectReturnType.get(0);
    }

    protected List<ExecutableElement> getMethodsWithReturnElement(final List<ExecutableElement> methods, final Element element) throws InvalidElementException {
        if(methods == null) throw new IllegalArgumentException("methods is null");
        if(element == null) throw new IllegalArgumentException("element is null");

        final TypeKind elementTypeKind = ElementUtils.getTypeKind(element);
        final String elementTypeName = ElementUtils.getDeclaredTypeName(element);

        return filter(methods, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                final TypeMirror returnType = item.getReturnType();

                if (!returnType.getKind().equals(elementTypeKind))
                    return false;

                switch (returnType.getKind()) {
                    case DECLARED:
                        final DeclaredType declaredType = (DeclaredType) returnType;
                        final TypeElement returnTypeElement = (TypeElement) declaredType.asElement();
                        final String returnTypeElementName = returnTypeElement.getQualifiedName().toString();

                        return returnTypeElementName.equals(elementTypeName);
                    default:
                        return true;
                }
            }
        });
    }

    protected String getNativeTypeForDatabase(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

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

                if(typeName.equals(ElementUtils.TYPE_STRING)) {
                    return "TEXT";
                }
                else {
                    throw new UnsupportedOperationException("this should only be called for native types and strings!");
                }
            default:
                throw new InvalidElementException(field.getSimpleName() + " is unknown", field);
        }
    }

    protected String getDatabaseFieldName(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

        FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
        if(fieldNameAnnotation == null)
            return field.getSimpleName().toString();

        String fieldName = fieldNameAnnotation.value();
        if(fieldName == null || fieldName.equals(""))
            throw new InvalidElementException("fieldName must not be null or empty!", field);

        return fieldName;
    }

    protected List<Element> getFieldsUsedInDatabase(TypeElement databaseEntityElement) throws InvalidElementException {
        if(databaseEntityElement == null) throw new IllegalArgumentException("databaseEntityElement is null");

        return filter(getElementsInTypeElement(databaseEntityElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.FIELD && isDatabaseField(item);
            }
        });
    }

    protected boolean isDatabaseField(Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");

        final Set<Modifier> modifiers = field.getModifiers();

        if(modifiers.contains(Modifier.STATIC))
            return false;

        if(modifiers.contains(Modifier.TRANSIENT))
            return false;

        if(field.getAnnotation(NotDatabaseField.class) != null)
            return false;

        return true;
    }

    public String getTableName(TypeElement databaseTypeElement) throws InvalidElementException {
        if(databaseTypeElement == null) throw new IllegalArgumentException("databaseTypeElement is null");

        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        if(annotation == null) throw new InvalidElementException("element not annotated with @DatabaseEntity", databaseTypeElement);

        String tableName = annotation.name();
        if(tableName == null || tableName.equals(""))
            return databaseTypeElement.getSimpleName().toString();

        return tableName;
    }

    private TypeElement getDatabaseEntityElementFromSecondParameter(ExecutableElement methodElement) {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        VariableElement secondParameter = methodElement.getParameters().get(1);
        return (TypeElement) ((DeclaredType) secondParameter.asType()).asElement();
    }

    private void checkHasVoidReturnType(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        if(methodElement.getReturnType().getKind() != TypeKind.VOID)
            throw new InvalidElementException("Only void is supported as return type for this method", methodElement);
    }

    private void checkHasTwoParameters(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters.size() != 2)
            throw new InvalidElementException("method must have exactly two parameters", methodElement);
    }

    private void checkSecondParameterMustBeDatabaseEntity(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters.size() < 2)
            throw new InvalidElementException("method must have at least two parameters where the first is of type android.database.sqlite.SQLiteDatabase and the second is a type annotated with @DatabaseEntity", methodElement);

        VariableElement secondParameter = parameters.get(1);
        if (secondParameter.asType().getKind() != TypeKind.DECLARED)
            throw new InvalidElementException("second parameter must be a declared type annotated with @DatabaseEntity", secondParameter);

        TypeElement parameterTypeElement = (TypeElement) ((DeclaredType) secondParameter.asType()).asElement();
        if(parameterTypeElement.getAnnotation(DatabaseEntity.class) == null)
            throw new InvalidElementException("second parameter must be annotated with @DatabaseEntity", secondParameter);
    }

    private void checkFirstParameterMustBeSQLiteDatabase(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters.size() < 1)
            throw new InvalidElementException("method must have at least one parameter where the first is of type android.database.sqlite.SQLiteDatabase", methodElement);

        VariableElement firstParameter = parameters.get(0);
        if (firstParameter.asType().getKind() != TypeKind.DECLARED)
            throw new InvalidElementException("first parameter must be the type android.database.sqlite.SQLiteDatabase", firstParameter);

        TypeElement parameterTypeElement = (TypeElement) ((DeclaredType) firstParameter.asType()).asElement();
        if (!QUALIFIED_NAME_SQLITE_DATABASE.equals(parameterTypeElement.getQualifiedName().toString()))
            throw new InvalidElementException("first parameter must be the type android.database.sqlite.SQLiteDatabase", firstParameter);
    }

    private void checkUniqueAnnotations(Class<? extends Annotation> annotationClass, ExecutableElement methodElement) throws InvalidElementException {
        if(annotationClass == null) throw new IllegalArgumentException("annotationClass is null");
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotation(annotationClass, Inject.class, methodElement);
        checkUniqueAnnotation(annotationClass, Replace.class, methodElement);
        checkUniqueAnnotation(annotationClass, Update.class, methodElement);
        checkUniqueAnnotation(annotationClass, Delete.class, methodElement);
        checkUniqueAnnotation(annotationClass, Select.class, methodElement);
        checkUniqueAnnotation(annotationClass, CreateTable.class, methodElement);
    }

    private void checkUniqueAnnotation(Class<? extends Annotation> annotationClass,
                                       Class<? extends Annotation> otherAnnotationClass,
                                       ExecutableElement methodElement) throws InvalidElementException {
        if(annotationClass == null) throw new IllegalArgumentException("annotationClass is null");
        if(otherAnnotationClass == null) throw new IllegalArgumentException("otherAnnotationClass is null");
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        if (!annotationClass.equals(otherAnnotationClass) && isAnnotationPresent(methodElement, otherAnnotationClass)) {
            throw new InvalidElementException(String.format("Method can't be annotated with both @%s and @%s",
                    annotationClass.getSimpleName(), otherAnnotationClass.getSimpleName()), methodElement);
        }
    }

    private boolean isAnnotationPresent(ExecutableElement methodElement, Class<? extends Annotation> annotationClass) {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");
        if(annotationClass == null) throw new IllegalArgumentException("annotationClass is null");

        return methodElement.getAnnotation(annotationClass) != null;
    }

    private boolean hasEmptyConstructor(TypeElement element) {
        if(element == null) throw new IllegalArgumentException("element is null");

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                if (executableElement.getParameters().size() == 0)
                    return true;
            }
        }
        return false;
    }

    private String getPackage(String qualifiedName) {
        if(qualifiedName == null) throw new IllegalArgumentException("qualifiedName is null");

        int lastDot = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, lastDot);
    }

    private TypeElement getSerializerElement(DatabaseStorage databaseStorage) {
        if(databaseStorage == null) throw new IllegalArgumentException("databaseStorage is null");

        try {
            databaseStorage.serializer();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement getCreateTableDatabaseEntity(CreateTable createTable) {
        if(createTable == null) throw new IllegalArgumentException("createTable is null");

        try {
            createTable.value();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement asTypeElement(TypeMirror typeMirror) {
        if(typeMirror == null) throw new IllegalArgumentException("typeMirror is null");

        Types TypeUtils = processingEnv.getTypeUtils();
        return (TypeElement) TypeUtils.asElement(typeMirror);
    }
}
