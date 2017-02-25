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
import java.util.List;
import java.util.Set;

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

import static net.daverix.slingerorm.compiler.ElementUtils.getElementsInTypeElement;
import static net.daverix.slingerorm.compiler.ListUtils.filter;

class DatabaseEntityModel {
    private final TypeElement databaseTypeElement;
    private final TypeElementConverter typeElementConverter;

    public DatabaseEntityModel(TypeElement databaseTypeElement, TypeElementConverter typeElementConverter) {
        this.databaseTypeElement = databaseTypeElement;
        this.typeElementConverter = typeElementConverter;
    }

    public String getTableName() throws InvalidElementException {
        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        if(annotation == null) throw new InvalidElementException("element not annotated with @DatabaseEntity", databaseTypeElement);

        String tableName = annotation.name();
        if(tableName.equals(""))
            return databaseTypeElement.getSimpleName().toString();

        return tableName;
    }

    public String[] getFieldNames() throws InvalidElementException {
        List<Element> elements = getFieldsUsedInDatabase();
        String[] names = new String[elements.size()];
        for(int i=0;i<names.length;i++) {
            names[i] = getDatabaseFieldName(elements.get(i));
        }
        return names;
    }

    private List<Element> getFieldsUsedInDatabase() throws InvalidElementException {
        return filter(getElementsInTypeElement(databaseTypeElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.FIELD && isDatabaseField(item);
            }
        });
    }

    private String getDatabaseFieldName(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

        FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
        if(fieldNameAnnotation == null)
            return field.getSimpleName().toString();

        String fieldName = fieldNameAnnotation.value();
        if(fieldName.equals(""))
            throw new InvalidElementException("fieldName must not be null or empty!", field);

        return fieldName;
    }

    public TypeElement getSerializerElement() {
        DatabaseEntity databaseEntity = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        try {
            databaseEntity.serializer();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return typeElementConverter.asTypeElement(mte.getTypeMirror());
        }
    }

    public String createTableSql() throws InvalidElementException {
        final List<Element> fields = getFieldsUsedInDatabase();
        if(fields.size() == 0)
            throw new InvalidElementException("no fields found in " + databaseTypeElement.getSimpleName(), databaseTypeElement);
        
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append("(");
        
        DatabaseEntity entityAnnotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String primaryKey = entityAnnotation.primaryKeyField();

        boolean primaryKeySet = false;

        for(int i=0;i<fields.size();i++) {
            final Element field = fields.get(i);
            final String fieldName = getDatabaseFieldName(field);
            final String fieldType = getDatabaseType(field);
            builder.append(fieldName).append(" ").append(fieldType);
            PrimaryKey annotation = field.getAnnotation(PrimaryKey.class);
            if(annotation != null || !primaryKey.equals("") && primaryKey.equals(field.getSimpleName().toString())) {
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

    public String getPrimaryKeyDbName() throws InvalidElementException {
        return getDatabaseFieldName(getPrimaryKeyField());
    }

    public Element getPrimaryKeyField() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();
        Element field = findSingleElementByAnnotation(fields, PrimaryKey.class);
        if (field == null)
            field = getPrimaryKeyFieldUsingDatabaseEntity(fields);

        if(field == null)
            throw new InvalidElementException("There must be a field annotated with PrimaryKey or the key specified in @DatabaseEntity is empty!", databaseTypeElement);

        return field;
    }

    private Element getPrimaryKeyFieldUsingDatabaseEntity(List<Element> validFields) throws InvalidElementException {
        if(validFields == null) throw new IllegalArgumentException("validFields");

        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String key = annotation.primaryKeyField();
        if(key.equals(""))
            return null;

        Element field = getFieldByName(validFields, key);
        if(field == null)
            throw new InvalidElementException("Field specified in DatabaseEntity annotation doesn't exist in entity class!", databaseTypeElement);

        return field;
    }

    private boolean isDatabaseField(Element field) {
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

    private String getDatabaseType(Element field) throws InvalidElementException {
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
                    TypeElement serializerTypeElement = getSerializerElement();
                    return getDatabaseTypeFromMethodParameterInSerializer(getDeserializeMethodsInSerializer(serializerTypeElement), field);
                }
            default:
                throw new InvalidElementException(field.getSimpleName() + " have a type not known by SlingerORM, solve this by creating a custom serializer", field);
        }
    }

    private String getDatabaseTypeFromMethodParameterInSerializer(List<ExecutableElement> methods, Element field) throws InvalidElementException {
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

    private String getNativeTypeForDatabase(Element field) throws InvalidElementException {
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

    private List<ExecutableElement> getDeserializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return filter(getMethodsInSerializer(serializerTypeElement), new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                return item.getAnnotation(DeserializeType.class) != null;
            }
        });
    }

    private ExecutableElement getMethodInSerializerThatMatchesReturnTypeElement(List<ExecutableElement> methods, Element field) throws InvalidElementException {
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

    private List<ExecutableElement> getMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return ElementUtils.getMethodsInTypeElement(serializerTypeElement);
    }

    private List<ExecutableElement> getMethodsWithReturnElement(final List<ExecutableElement> methods, final Element element) throws InvalidElementException {
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

    private Element getFieldByName(List<Element> fields, final String name) throws InvalidElementException {
        if(fields == null) throw new IllegalArgumentException("fields is null");
        if(name == null) throw new IllegalArgumentException("name is null");

        return ListUtils.firstOrDefault(filter(fields, new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return name.equals(item.getSimpleName().toString());
            }
        }));
    }

    private FieldMethod findGetter(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
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
                return findDirectGetter(field);
            case OTHER:
                return findGetterInSerializer(serializerTypeElement, field);
            default:
                throw new UnsupportedOperationException("this should not be called!");
        }
    }

    public FieldMethod findDirectGetter(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

        List<ExecutableElement> methodsInDatabaseEntityElement = ElementUtils.getMethodsInTypeElement(databaseTypeElement);
        ExecutableElement method = findMethodByFieldNameAndGetFieldAnnotation(methodsInDatabaseEntityElement, field.getSimpleName().toString());
        if(method != null)
            return new FieldMethodImpl("item." + method.getSimpleName() + "()");

        boolean isBoolean = field.asType().getKind() == TypeKind.BOOLEAN;
        method = findMethodByFieldNameOnly(methodsInDatabaseEntityElement, field.getSimpleName().toString(), isBoolean ? "is" : "get");
        if(method != null)
            return new FieldMethodImpl("item." + method.getSimpleName() + "()");

        if (!ElementUtils.isAccessible(field))
            throw new InvalidElementException("No get method or a public field for " + field.getSimpleName() + " in " + databaseTypeElement.getSimpleName(),
                    databaseTypeElement);

        return new FieldMethodImpl("item." + field.getSimpleName().toString());
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
            if(fieldReference.equals(""))
                throw new InvalidElementException(element.getSimpleName() + " has a GetField annotation with empty value!", element);

            if(fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    private FieldMethod findGetterInSerializer(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        final List<ExecutableElement> methodsInSerializer = getSerializeMethodsInSerializer(serializerTypeElement);
        final List<ExecutableElement> methods = getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(methodsInSerializer, field);

        if(methods.size() == 0) {
            throw new InvalidElementException("No method found for field " + field.asType(), field);
        }

        if(methods.size() > 1) {
            throw new InvalidElementException("Only one @SerializeType method per parameter type supported, found " + methods.size() + " methods with same parameter type in " + field.asType(),
                    methods.get(0));
        }

        findDirectGetter(field);

        FieldMethod getter = findDirectGetter(field);
        return new WrappedFieldMethod("serializer." + methods.get(0).getSimpleName() + "(", getter, ")");
    }

    private List<ExecutableElement> getSerializeMethodsInSerializer(TypeElement serializerTypeElement) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");

        return filter(getMethodsInSerializer(serializerTypeElement), new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                return item.getAnnotation(SerializeType.class) != null;
            }
        });
    }

    private List<ExecutableElement> getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(List<ExecutableElement> methods, final Element element) throws InvalidElementException {
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

                if (!typeKind.equals(parameterTypeKind))
                    return false;

                ObjectType paramterObjectType = getObjectTypeForElement(parameterTypeKind, element);
                if (paramterObjectType != ObjectType.OTHER)
                    return false;

                final DeclaredType parameterDeclaredType = (DeclaredType) variableElement.asType();
                final TypeElement parameterTypeElement = (TypeElement) parameterDeclaredType.asElement();
                final String parameterTypeName = parameterTypeElement.getQualifiedName().toString();

                return parameterTypeName.equals(elementTypeName);
            }
        });
    }

    private ObjectType getObjectTypeForElement(TypeKind typeKind, Element element) throws InvalidElementException {
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
            case ARRAY:
            case BYTE:
            case CHAR:
                throw new InvalidElementException(typeKind + " is not known by SlingerORM, solve this by creating a custom serializer method", element);
            default:
                throw new IllegalStateException(typeKind + " should never been reached, processor error?");
        }
    }

    private ExecutableElement findMethodByFieldNameAndSetFieldAnnotation(List<ExecutableElement> elements, String fieldName) throws InvalidElementException {
        for(ExecutableElement element : elements) {
            SetField annotation = element.getAnnotation(SetField.class);
            if(annotation == null)
                continue;

            String fieldReference = annotation.value();
            if(fieldReference.equals(""))
                throw new InvalidElementException(element.getSimpleName() + " has a SetField annotation with empty value!", element);

            if(fieldReference.equals(fieldName))
                return element;
        }

        return null;
    }

    private FieldMethod findSetter(TypeElement serializerTypeElement, Element field) throws InvalidElementException {
        if(serializerTypeElement == null) throw new IllegalArgumentException("serializerTypeElement is null");
        if(field == null) throw new IllegalArgumentException("field is null");

        List<ExecutableElement> methodsInDatabaseEntityElement = ElementUtils.getMethodsInTypeElement(databaseTypeElement);
        ExecutableElement method = findMethodByFieldNameAndSetFieldAnnotation(methodsInDatabaseEntityElement, field.getSimpleName().toString());
        if(method != null) {
            return findSetterMethodFromParameter(serializerTypeElement, method, field);
        }

        method = findMethodByFieldNameOnly(methodsInDatabaseEntityElement, field.getSimpleName().toString(), "set");
        if(method != null) {
            return findSetterMethodFromParameter(serializerTypeElement, method, field);
        }

        if (!ElementUtils.isAccessible(field))
            throw new InvalidElementException("No get method or a public field for " + field.getSimpleName() + " in " + databaseTypeElement.getSimpleName(), field);

        return new WrappedFieldMethod(field.getSimpleName().toString() + " = ", findCursorMethod(serializerTypeElement, field, field), "");
    }

    private FieldMethod findSetterMethodFromParameter(TypeElement serializerTypeElement, ExecutableElement method, Element field) throws InvalidElementException {
        if(method == null) throw new IllegalArgumentException("method is null");

        List<? extends VariableElement> typeParameters = method.getParameters();
        if(typeParameters.size() != 1)
            throw new InvalidElementException(String.format("method has %d parameters, only 1 parameter supported!", typeParameters.size()), method);

        return new WrappedFieldMethod(method.getSimpleName() + "(", findCursorMethod(serializerTypeElement, typeParameters.get(0), field), ")");
    }

    private FieldMethod findCursorMethod(TypeElement serializerTypeElement, Element element, Element field) throws InvalidElementException {
        if(element == null) throw new IllegalArgumentException("element is null");

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

        List<ExecutableElement> methods = filter(methodsInSerializer, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) throws InvalidElementException {
                TypeElement returnTypeElement = typeElementConverter.asTypeElement(item.getReturnType());
                return typeElement.getQualifiedName().equals(returnTypeElement.getQualifiedName());
            }
        });
        if(methods.size() == 0)
            throw new InvalidElementException("SlingerORM doesn't know how to handle the type " + typeElement + ", consider providing your custom method in the serializer", element);

        ExecutableElement method = methods.get(0);
        List<? extends VariableElement> parameters = method.getParameters();
        if(parameters.size() != 1)
            throw new InvalidElementException("Methods in the serializer must have only one parameter!", method);

        VariableElement parameter = parameters.get(0);
        TypeKind typeKind = ElementUtils.getTypeKind(parameter);
        if(!typeKind.isPrimitive() && getObjectTypeForElement(typeKind, parameter) != ObjectType.STRING)
            throw new InvalidElementException("Only primitive types are supported as parameter in deserialize methods", method);

        return new WrappedFieldMethod("serializer." + method.getSimpleName() + "(", findCursorMethod(serializerTypeElement, parameter, field), ")");
    }

    private String getColumnIndex(Element field) throws InvalidElementException {
        return "cursor.getColumnIndex(\"" + getDatabaseFieldName(field) + "\")";
    }

    public List<FieldMethod> getSetters() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();
        List<FieldMethod> setters = new ArrayList<FieldMethod>();
        for(Element field : fields) {
            setters.add(createSetter(field));
        }
        return setters;
    }

    private FieldMethod createSetter(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

        TypeElement serializerTypeElement = getSerializerElement();
        return findSetter(serializerTypeElement, field);
    }

    public List<FieldMethod> getGetters() throws InvalidElementException {
        List<Element> fields = getFieldsUsedInDatabase();
        List<FieldMethod> setters = new ArrayList<FieldMethod>();
        for(Element field : fields) {
            setters.add(createGetter(field));
        }
        return setters;
    }

    private FieldMethod createGetter(Element field) throws InvalidElementException {
        if(field == null) throw new IllegalArgumentException("field is null");

        TypeElement serializerTypeElement = getSerializerElement();
        return new WrappedFieldMethod("\"" + getDatabaseFieldName(field) + "\", ", findGetter(serializerTypeElement, field), "");
    }

    public String getItemSql() throws InvalidElementException {
        return getPrimaryKeyDbName() + "=?";
    }

    public String getItemSqlArgs() throws InvalidElementException {
        Element primaryKeyField = getPrimaryKeyField();
        FieldMethod directGetter = findDirectGetter(primaryKeyField);
        if(ElementUtils.isString(primaryKeyField)) {
            return "new String[]{" + directGetter.getMethod() + "}";
        }
        else {
            return "new String[]{String.valueOf(" + directGetter.getMethod() + ")}";
        }
    }

    private class WrappedFieldMethod implements FieldMethod {
        private final String prefix;
        private final FieldMethod fieldMethod;
        private final String suffix;

        public WrappedFieldMethod(String prefix, FieldMethod fieldMethod, String suffix) {
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

        public FieldMethodImpl(String method) {
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}
