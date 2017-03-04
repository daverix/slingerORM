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

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.DeserializeType;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.NotDatabaseField;
import net.daverix.slingerorm.annotation.PrimaryKey;
import net.daverix.slingerorm.annotation.SerializeType;
import net.daverix.slingerorm.annotation.SetField;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.daverix.slingerorm.compiler.ElementUtils.filter;
import static net.daverix.slingerorm.compiler.ElementUtils.getElementsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.getMethodsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.map;

class DatabaseEntityModel {
    private final TypeElement databaseTypeElement;
    private final TypeElementConverter typeElementConverter;

    DatabaseEntityModel(TypeElement databaseTypeElement, TypeElementConverter typeElementConverter) {
        this.databaseTypeElement = databaseTypeElement;
        this.typeElementConverter = typeElementConverter;
    }

    String getTableName() throws InvalidElementException {
        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        if (annotation == null)
            throw new InvalidElementException("element not annotated with @DatabaseEntity", databaseTypeElement);

        String tableName = annotation.name();
        if (tableName.equals(""))
            return databaseTypeElement.getSimpleName().toString();

        return tableName;
    }

    String[] getFieldNames() throws InvalidElementException {
        List<Element> elements = getFieldsUsedInDatabase();
        String[] names = new String[elements.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = getDatabaseFieldName(elements.get(i));
        }
        return names;
    }

    private List<Element> getFieldsUsedInDatabase() throws InvalidElementException {
        return filter(getElementsInTypeElement(databaseTypeElement), this::isDatabaseField);
    }

    private List<String> getDatabaseFieldNames(List<Element> fields) throws InvalidElementException {
        List<String> names = new ArrayList<>();

        for (Element field : fields) {
            names.add(getDatabaseFieldName(field));
        }

        return names;
    }

    private String getDatabaseFieldName(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

        FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
        if (fieldNameAnnotation == null)
            return field.getSimpleName().toString();

        String fieldName = fieldNameAnnotation.value();
        if (fieldName.equals(""))
            throw new InvalidElementException("fieldName must not be null or empty!", field);

        return fieldName;
    }

    TypeElement getSerializerElement() {
        DatabaseEntity databaseEntity = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        try {
            databaseEntity.serializer();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return typeElementConverter.asTypeElement(mte.getTypeMirror());
        }
    }

    String createTableSql() throws InvalidElementException {
        final List<Element> fields = getFieldsUsedInDatabase();
        if (fields.size() == 0)
            throw new InvalidElementException("no fields found in " + databaseTypeElement.getSimpleName(), databaseTypeElement);

        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append("(");

        Map<String, String> dbFieldNames = new HashMap<>();
        Map<String, String> fieldTypes = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            Element field = fields.get(i);
            String simpleName = field.getSimpleName().toString();
            dbFieldNames.put(simpleName, getDatabaseFieldName(field));
            fieldTypes.put(simpleName, getDatabaseType(field));
        }

        Set<String> primaryKeysCollection = getPrimaryKeyFieldNames();

        if (primaryKeysCollection.isEmpty())
            throw new InvalidElementException("Primary key not found when creating SQL for entity " + databaseTypeElement.getSimpleName(), databaseTypeElement);

        final int primaryKeysCollectionSize = primaryKeysCollection.size();
        for (int i = 0; i < fields.size(); i++) {
            final Element field = fields.get(i);
            String fieldName = field.getSimpleName().toString();

            builder.append(dbFieldNames.get(fieldName))
                    .append(" ")
                    .append(fieldTypes.get(fieldName));

            boolean containsFieldName = primaryKeysCollection.contains(fieldName);

            if (primaryKeysCollectionSize == 1 && containsFieldName) {
                builder.append(" NOT NULL PRIMARY KEY");
            } else if (containsFieldName) {
                builder.append(" NOT NULL");
            }

            if (i < fields.size() - 1) {
                builder.append(", ");
            }
        }

        if (primaryKeysCollectionSize > 1) {
            List<String> dbNames = primaryKeysCollection.stream()
                    .map(dbFieldNames::get)
                    .collect(Collectors.toList());

            builder.append(", PRIMARY KEY(")
                    .append(String.join(",", dbNames))
                    .append(")");
        }

        builder.append(")");

        return builder.toString();
    }

    private Set<String> getPrimaryKeyFieldNames() throws InvalidElementException {
        DatabaseEntity entityAnnotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String[] primaryKeys = entityAnnotation.primaryKeyFields();
        Set<String> annotationKeys = Arrays.stream(primaryKeys)
                .filter(x -> !x.isEmpty())
                .collect(toSet());

        if (annotationKeys.size() > 0) {
            return annotationKeys;
        } else {
            return getPrimaryKeyFields()
                    .stream()
                    .map(Element::getSimpleName)
                    .map(Name::toString)
                    .filter(x -> !x.isEmpty())
                    .collect(toSet());
        }
    }

    private List<String> getPrimaryKeyDbNames() throws InvalidElementException {
        return getDatabaseFieldNames(getPrimaryKeyFields());
    }

    private List<Element> getPrimaryKeyFields() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();

        List<Element> primaryKeysByAnnotation = findElementsByAnnotation(fields, PrimaryKey.class);
        if (primaryKeysByAnnotation.isEmpty())
            fields = getPrimaryKeyFieldsUsingDatabaseEntity(fields);

        if (fields == null || fields.isEmpty())
            throw new InvalidElementException("There must be a field annotated with PrimaryKey or the keys specified in @DatabaseEntity is empty!", databaseTypeElement);

        return fields;
    }

    private List<Element> getPrimaryKeyFieldsUsingDatabaseEntity(List<Element> validFields) throws InvalidElementException {
        if (validFields == null) throw new IllegalArgumentException("validFields");

        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String[] keys = annotation.primaryKeyFields();
        if (keys.length == 0)
            return null;

        List<Element> elements = new ArrayList<>();
        for (String key : keys) {
            Element field = getFieldByName(validFields, key);
            if (field == null)
                throw new InvalidElementException("Field specified in DatabaseEntity annotation doesn't exist in entity class!", databaseTypeElement);

            elements.add(field);
        }

        return elements;
    }

    private boolean isDatabaseField(Element element) {
        if (element == null)
            throw new IllegalArgumentException("element is null");

        if (element.getKind() != ElementKind.FIELD)
            return false;

        final Set<Modifier> modifiers = element.getModifiers();

        return !modifiers.contains(Modifier.STATIC) &&
                !modifiers.contains(Modifier.TRANSIENT) &&
                element.getAnnotation(NotDatabaseField.class) == null;

    }

    private String getDatabaseType(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

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

                if (typeName.equals(ElementUtils.TYPE_STRING)) {
                    return getNativeTypeForDatabase(field);
                } else {
                    TypeElement serializerTypeElement = getSerializerElement();
                    return getDatabaseTypeFromMethodParameterInSerializer(getDeserializeMethodsInSerializer(serializerTypeElement), field);
                }
            default:
                throw new InvalidElementException(field.getSimpleName() + " have a type not known by SlingerORM, solve this by creating a custom serializer", field);
        }
    }

    private String getDatabaseTypeFromMethodParameterInSerializer(List<ExecutableElement> methods, Element field) throws InvalidElementException {
        if (methods == null) throw new IllegalArgumentException("methods is null");
        if (field == null) throw new IllegalArgumentException("field is null");

        final ExecutableElement serializerMethod = getMethodInSerializerThatMatchesReturnTypeElement(methods, field);
        final TypeMirror typeMirror = serializerMethod.getReturnType();
        final TypeKind typeKind = typeMirror.getKind();
        if (typeKind != TypeKind.DECLARED) {
            throw new InvalidElementException("TypeKind should be declared but was " + typeKind, serializerMethod);
        }

        final DeclaredType declaredType = (DeclaredType) typeMirror;
        final TypeElement typeElement = (TypeElement) declaredType.asElement();
        final String typeName = typeElement.getQualifiedName().toString();
        if (typeName.equals(ElementUtils.TYPE_STRING)) {
            throw new InvalidElementException("String type should not be passed to this method", serializerMethod);
        }

        final List<? extends VariableElement> parameters = serializerMethod.getParameters();
        if (parameters.size() != 1) {
            throw new InvalidElementException("@DeserializeType/@SerializeType methods must have one parameter with a native type or String, got " + parameters.size() + " parameters for method " + serializerMethod.getSimpleName(), serializerMethod);
        }

        return getNativeTypeForDatabase(parameters.get(0));
    }

    private String getNativeTypeForDatabase(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

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

                if (typeName.equals(ElementUtils.TYPE_STRING)) {
                    return "TEXT";
                } else {
                    throw new UnsupportedOperationException("this should only be called for native types and strings!");
                }
            default:
                throw new InvalidElementException(field.getSimpleName() + " is unknown", field);
        }
    }

    private List<ExecutableElement> getDeserializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");

        return filter(getMethodsInSerializer(serializerTypeElement),
                item -> item.getAnnotation(DeserializeType.class) != null);
    }

    private ExecutableElement getMethodInSerializerThatMatchesReturnTypeElement(List<ExecutableElement> methods, Element field) throws InvalidElementException {
        if (methods == null) throw new IllegalArgumentException("methods is null");
        if (field == null) throw new IllegalArgumentException("field is null");

        final String typeName = ElementUtils.getDeclaredTypeName(field);
        final List<ExecutableElement> methodsWithCorrectReturnType = getMethodsWithReturnElement(methods, field);
        if (methodsWithCorrectReturnType.size() < 1) {
            throw new InvalidElementException("No @DeserializeType methods found with return type " + typeName, field);
        }

        if (methodsWithCorrectReturnType.size() > 1) {
            throw new InvalidElementException("Return value of @DeserializeType/@SerializeType methods must be unique, there are " + methodsWithCorrectReturnType.size() + " with return type " + typeName, field);
        }

        return methodsWithCorrectReturnType.get(0);
    }

    private List<ExecutableElement> getMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");

        return getMethodsInTypeElement(serializerTypeElement);
    }

    private List<ExecutableElement> getMethodsWithReturnElement(final List<ExecutableElement> methods, final Element element) throws InvalidElementException {
        if (methods == null) throw new IllegalArgumentException("methods is null");
        if (element == null) throw new IllegalArgumentException("element is null");

        final TypeKind elementTypeKind = ElementUtils.getTypeKind(element);
        final String elementTypeName = ElementUtils.getDeclaredTypeName(element);

        return methods.stream().filter(item -> {
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
        }).collect(toList());
    }

    private List<Element> findElementsByAnnotation(List<Element> elements,
                                                   final Class<? extends Annotation> annotationClass) throws InvalidElementException {
        if (elements == null) throw new IllegalArgumentException("elements is null");
        if (annotationClass == null) throw new IllegalArgumentException("annotationClass is null");

        return elements.stream()
                .filter(item -> item.getAnnotation(annotationClass) != null)
                .collect(toList());
    }

    private Element getFieldByName(List<Element> fields, final String name) throws InvalidElementException {
        if (fields == null) throw new IllegalArgumentException("fields is null");
        if (name == null) throw new IllegalArgumentException("name is null");

        return fields.stream()
                .filter(item -> name.equals(item.getSimpleName().toString()))
                .findFirst().orElse(null);
    }

    private FieldMethod findGetter(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");
        if (field == null) throw new IllegalArgumentException("field is null");

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
                return findDirectGetter(field);
            case OTHER:
                return findGetterInSerializer(serializerTypeElement, field);
            default:
                throw new UnsupportedOperationException("this should not be called!");
        }
    }

    private FieldMethod findDirectGetter(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

        List<ExecutableElement> methodsInDatabaseEntityElement = getMethodsInTypeElement(databaseTypeElement);
        ExecutableElement method = findMethodByFieldNameAndGetFieldAnnotation(methodsInDatabaseEntityElement, field.getSimpleName().toString());
        if (method != null)
            return new FieldMethodImpl("item." + method.getSimpleName() + "()");

        boolean isBoolean = field.asType().getKind() == TypeKind.BOOLEAN;
        method = findMethodByFieldNameOnly(methodsInDatabaseEntityElement, field.getSimpleName().toString(), isBoolean ? "is" : "get");
        if (method != null)
            return new FieldMethodImpl("item." + method.getSimpleName() + "()");

        if (!ElementUtils.isAccessible(field))
            throw new InvalidElementException("No get method or a public field for " + field.getSimpleName() + " in " + databaseTypeElement.getSimpleName(),
                    databaseTypeElement);

        return new FieldMethodImpl("item." + field.getSimpleName().toString());
    }

    private ExecutableElement findMethodByFieldNameOnly(List<ExecutableElement> methods, final String fieldName, final String prefix) throws InvalidElementException {
        if (methods == null) throw new IllegalArgumentException("methods is null");
        if (fieldName == null) throw new IllegalArgumentException("fieldName is null");
        if (prefix == null) throw new IllegalArgumentException("prefix is null");

        return methods.stream()
                .filter(item -> {
                    String firstLetter = fieldName.substring(0, 1).toUpperCase();
                    String methodName = prefix + firstLetter + fieldName.substring(1);

                    return methodName.equals(item.getSimpleName().toString());
                })
                .findFirst().orElse(null);
    }

    private ExecutableElement findMethodByFieldNameAndGetFieldAnnotation(List<ExecutableElement> elements, String fieldName) throws InvalidElementException {
        if (elements == null) throw new IllegalArgumentException("elements is null");
        if (fieldName == null) throw new IllegalArgumentException("fieldName is null");

        for (ExecutableElement element : elements) {
            GetField annotation = element.getAnnotation(GetField.class);
            if (annotation == null)
                continue;

            String fieldReference = annotation.value();
            if (fieldReference.equals(""))
                throw new InvalidElementException(element.getSimpleName() + " has a GetField annotation with empty value!", element);

            if (fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    private FieldMethod findGetterInSerializer(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");
        if (field == null) throw new IllegalArgumentException("field is null");

        final List<ExecutableElement> methodsInSerializer = getSerializeMethodsInSerializer(serializerTypeElement);
        final List<ExecutableElement> methods = getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(methodsInSerializer, field);

        if (methods.size() == 0) {
            throw new InvalidElementException("No method found for field " + field.asType(), field);
        }

        if (methods.size() > 1) {
            throw new InvalidElementException("Only one @SerializeType method per parameter type supported, found " + methods.size() + " methods with same parameter type in " + field.asType(),
                    methods.get(0));
        }

        findDirectGetter(field);

        FieldMethod getter = findDirectGetter(field);
        return new WrappedFieldMethod("serializer." + methods.get(0).getSimpleName() + "(", getter, ")");
    }

    private List<ExecutableElement> getSerializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");

        return getMethodsInSerializer(serializerTypeElement)
                .stream()
                .filter(item -> item.getAnnotation(SerializeType.class) != null)
                .collect(toList());
    }

    private List<ExecutableElement> getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(List<ExecutableElement> methods, final Element element) throws InvalidElementException {
        if (methods == null) throw new IllegalArgumentException("methods is null");
        if (element == null) throw new IllegalArgumentException("element is null");

        final TypeKind typeKind = ElementUtils.getTypeKind(element);
        final ObjectType objectType = getObjectTypeForElement(typeKind, element);
        if (objectType != ObjectType.OTHER)
            throw new InvalidElementException("Element should be a declared type", element);

        final DeclaredType parameterDeclaredType = (DeclaredType) element.asType();
        final TypeElement parameterTypeElement = (TypeElement) parameterDeclaredType.asElement();
        final String elementTypeName = parameterTypeElement.getQualifiedName().toString();

        return filter(methods, item -> {
            final List<? extends VariableElement> parameters = item.getParameters();
            if (parameters.size() != 1) {
                throw new InvalidElementException("@DeserializeType/@SerializeType methods must have one parameter, got " + parameters.size() + " parameters for method " + item.getSimpleName(), item);
            }

            final VariableElement variableElement = parameters.get(0);
            final TypeKind parameterTypeKind = ElementUtils.getTypeKind(variableElement);

            if (!typeKind.equals(parameterTypeKind))
                return false;

            ObjectType paramterObjectType = getObjectTypeForElement(parameterTypeKind, element);
            if (paramterObjectType != ObjectType.OTHER)
                return false;

            final DeclaredType parameterDeclaredType1 = (DeclaredType) variableElement.asType();
            final TypeElement parameterTypeElement1 = (TypeElement) parameterDeclaredType1.asElement();
            final String parameterTypeName = parameterTypeElement1.getQualifiedName().toString();

            return parameterTypeName.equals(elementTypeName);
        });
    }

    private ObjectType getObjectTypeForElement(TypeKind typeKind, Element element) throws InvalidElementException {
        if (typeKind == null) throw new IllegalArgumentException("typeKind is null");
        if (element == null) throw new IllegalArgumentException("element is null");

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
                if (typeName.equals(ElementUtils.TYPE_STRING)) {
                    return ObjectType.STRING;
                } else {
                    return ObjectType.OTHER;
                }
            case ARRAY:
            case BYTE:
            case CHAR:
                throw new InvalidElementException(typeKind + " is not known by SlingerORM, solve this by creating a custom serializer method", element);
            default:
                throw new IllegalStateException(typeKind + " should never been reached, processor error?");
        }
    }

    private ExecutableElement findMethodByFieldNameAndSetFieldAnnotation(List<ExecutableElement> elements, String fieldName) throws InvalidElementException {
        for (ExecutableElement element : elements) {
            SetField annotation = element.getAnnotation(SetField.class);
            if (annotation == null)
                continue;

            String fieldReference = annotation.value();
            if (fieldReference.equals(""))
                throw new InvalidElementException(element.getSimpleName() + " has a SetField annotation with empty value!", element);

            if (fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    private FieldMethod findSetter(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if (serializerTypeElement == null)
            throw new IllegalArgumentException("serializerTypeElement is null");
        if (field == null) throw new IllegalArgumentException("field is null");

        List<ExecutableElement> methodsInDatabaseEntityElement = getMethodsInTypeElement(databaseTypeElement);
        ExecutableElement method = findMethodByFieldNameAndSetFieldAnnotation(methodsInDatabaseEntityElement, field.getSimpleName().toString());
        if (method != null) {
            return findSetterMethodFromParameter(serializerTypeElement, method, field);
        }

        method = findMethodByFieldNameOnly(methodsInDatabaseEntityElement, field.getSimpleName().toString(), "set");
        if (method != null) {
            return findSetterMethodFromParameter(serializerTypeElement, method, field);
        }

        if (!ElementUtils.isAccessible(field))
            throw new InvalidElementException("No get method or a public field for " + field.getSimpleName() + " in " + databaseTypeElement.getSimpleName(), field);

        return new WrappedFieldMethod(field.getSimpleName().toString() + " = ", findCursorMethod(serializerTypeElement, field, field), "");
    }

    private FieldMethod findSetterMethodFromParameter(TypeElement serializerTypeElement, ExecutableElement method, Element field) throws InvalidElementException {
        if (method == null) throw new IllegalArgumentException("method is null");

        List<? extends VariableElement> typeParameters = method.getParameters();
        if (typeParameters.size() != 1)
            throw new InvalidElementException(String.format(Locale.ENGLISH,
                    "method has %d parameters, only 1 parameter supported!",
                    typeParameters.size()), method);

        return new WrappedFieldMethod(method.getSimpleName() + "(", findCursorMethod(serializerTypeElement, typeParameters.get(0), field), ")");
    }

    private FieldMethod findCursorMethod(TypeElement serializerTypeElement, Element element, Element field) throws InvalidElementException {
        if (element == null) throw new IllegalArgumentException("element is null");

        final TypeKind fieldTypeKind = ElementUtils.getTypeKind(element);
        final ObjectType objectType = getObjectTypeForElement(fieldTypeKind, element);

        switch (objectType) {
            case BOOLEAN:
                return new FieldMethodImpl("cursor.getShort(" + getColumnIndex(field) + ") == 1");
            case DOUBLE:
                return new FieldMethodImpl("cursor.getDouble(" + getColumnIndex(field) + ")");
            case FLOAT:
                return new FieldMethodImpl("cursor.getFloat(" + getColumnIndex(field) + ")");
            case INT:
                return new FieldMethodImpl("cursor.getInt(" + getColumnIndex(field) + ")");
            case LONG:
                return new FieldMethodImpl("cursor.getLong(" + getColumnIndex(field) + ")");
            case SHORT:
                return new FieldMethodImpl("cursor.getShort(" + getColumnIndex(field) + ")");
            case STRING:
                return new FieldMethodImpl("cursor.getString(" + getColumnIndex(field) + ")");
            case OTHER:
                return findSetterInSerializer(serializerTypeElement, element, field);
            default:
                throw new UnsupportedOperationException("this should not be called!");
        }
    }

    private FieldMethod findSetterInSerializer(TypeElement serializerTypeElement, Element element, Element field) throws InvalidElementException {
        List<ExecutableElement> methodsInSerializer = getDeserializeMethodsInSerializer(serializerTypeElement);
        final TypeElement typeElement = ElementUtils.getTypeElement(element);

        List<ExecutableElement> methods = filter(methodsInSerializer, item -> {
            TypeElement returnTypeElement = typeElementConverter.asTypeElement(item.getReturnType());
            return typeElement.getQualifiedName().equals(returnTypeElement.getQualifiedName());
        });

        if (methods.size() == 0)
            throw new InvalidElementException("SlingerORM doesn't know how to handle the type " + typeElement + ", consider providing your custom method in the serializer", element);

        ExecutableElement method = methods.get(0);
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != 1)
            throw new InvalidElementException("Methods in the serializer must have only one parameter!", method);

        VariableElement parameter = parameters.get(0);
        TypeKind typeKind = ElementUtils.getTypeKind(parameter);
        if (!typeKind.isPrimitive() && getObjectTypeForElement(typeKind, parameter) != ObjectType.STRING)
            throw new InvalidElementException("Only primitive types are supported as parameter in deserialize methods", method);

        return new WrappedFieldMethod("serializer." + method.getSimpleName() + "(", findCursorMethod(serializerTypeElement, parameter, field), ")");
    }

    private String getColumnIndex(Element field) throws InvalidElementException {
        return "cursor.getColumnIndex(\"" + getDatabaseFieldName(field) + "\")";
    }

    List<FieldMethod> getSetters() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();
        List<FieldMethod> setters = new ArrayList<>();
        for (Element field : fields) {
            setters.add(createSetter(field));
        }
        return setters;
    }

    private FieldMethod createSetter(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

        TypeElement serializerTypeElement = getSerializerElement();
        return findSetter(serializerTypeElement, field);
    }

    List<FieldMethod> getGetters() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();
        List<FieldMethod> setters = new ArrayList<>();
        for (Element field : fields) {
            setters.add(createGetter(field));
        }
        return setters;
    }

    private FieldMethod createGetter(Element field) throws InvalidElementException {
        if (field == null) throw new IllegalArgumentException("field is null");

        TypeElement serializerTypeElement = getSerializerElement();
        return new WrappedFieldMethod("\"" + getDatabaseFieldName(field) + "\", ", findGetter(serializerTypeElement, field), "");
    }

    String getItemSql() throws InvalidElementException {
        return getPrimaryKeyDbNames()
                .stream()
                .map(key -> key + " = ?")
                .reduce((a, b) -> a + " AND " + b)
                .orElse("");
    }

    List<String> getItemSqlArgs() throws InvalidElementException {
        return map(getPrimaryKeyFields(), primaryKeyField -> {
            FieldMethod directGetter = findDirectGetter(primaryKeyField);
            if (ElementUtils.isString(primaryKeyField)) {
                return directGetter.getMethod();
            } else {
                return "String.valueOf(" + directGetter.getMethod() + ")";
            }
        });
    }

    private class WrappedFieldMethod implements FieldMethod {
        private final String prefix;
        private final FieldMethod fieldMethod;
        private final String suffix;

        WrappedFieldMethod(String prefix, FieldMethod fieldMethod, String suffix) {
            this.prefix = prefix;
            this.fieldMethod = fieldMethod;
            this.suffix = suffix;
        }

        @Override
        public String getMethod() {
            return prefix + fieldMethod.getMethod() + suffix;
        }
    }

    private class FieldMethodImpl implements FieldMethod {
        private final String method;

        FieldMethodImpl(String method) {
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}
