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

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.DeserializeType;
import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.NotDatabaseField;
import net.daverix.slingerorm.annotation.PrimaryKey;
import net.daverix.slingerorm.annotation.SerializeType;
import net.daverix.slingerorm.annotation.SetField;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_STRING;
import static net.daverix.slingerorm.compiler.ElementUtils.getDeclaredTypeName;
import static net.daverix.slingerorm.compiler.ElementUtils.getElementsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.getMethodsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.getTypeKind;
import static net.daverix.slingerorm.compiler.ElementUtils.isAccessable;
import static net.daverix.slingerorm.compiler.ListUtils.filter;
import static net.daverix.slingerorm.compiler.ListUtils.firstOrDefault;
import static net.daverix.slingerorm.compiler.ListUtils.mapItems;

public class EntityType {
    private final TypeElementConverter mTypeElementConverter;
    private final TypeElement mTypeElement;

    private List<Element> mElementsInType;
    private List<Element> mFields;
    private List<ExecutableElement> mMethods;

    private TypeElement mSerializer;
    private List<ExecutableElement> mMethodsInSerializer;
    private List<ExecutableElement> mSerializeMethodsInSerializer;
    private List<ExecutableElement> mDeserializeMethodsInSerializer;

    public EntityType(TypeElement typeElement, TypeElementConverter typeElementConverter) {
        if(typeElement == null) throw new IllegalArgumentException("typeElement is null");
        if(typeElementConverter == null) throw new IllegalArgumentException("typeElementConverter is null");

        mTypeElement = typeElement;
        mTypeElementConverter = typeElementConverter;
    }

    public String getName() {
        return mTypeElement.getSimpleName().toString();
    }

    public String getMapperTypeName() {
        return mTypeElement.getSimpleName().toString() + "Storage";
    }

    public String getPrimaryKeyDbName() {
        return getDatabaseFieldName(getPrimaryKeyField());
    }

    public String getTableName() {
        DatabaseEntity annotation = mTypeElement.getAnnotation(DatabaseEntity.class);
        String tableName = annotation.name();
        if(tableName == null || tableName.equals(""))
            return mTypeElement.getSimpleName().toString();

        return tableName;
    }

    public String getSerializerTypeName() {
        return getSerializerType().getSimpleName().toString();
    }

    public String getSerializerQualifiedName() {
        return getSerializerType().getQualifiedName().toString();
    }

    public List<String> getSetterMethodParts() {
        return mapItems(getFieldsUsedInDatabase(), new Function<String, Element>() {
            @Override
            public String apply(Element field) {
                return getSetterWithParam(field);
            }
        });
    }

    public Map<String,String> getFieldMethodGetter() {
        Map<String, String> fieldMethodGetterParts = new HashMap<String, String>();
        for(Element field : getFieldsUsedInDatabase()) {
            final String fieldName = getDatabaseFieldName(field);
            final String getter = findGetter(field);
            fieldMethodGetterParts.put(fieldName, getter);
        }
        return fieldMethodGetterParts;
    }

    public String createTableSql() {
        final List<Element> fields = getFieldsUsedInDatabase();
        if(getFieldsUsedInDatabase().size() == 0)
            throw new IllegalArgumentException("no fields found in " + mTypeElement.getSimpleName());

        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append("(");

        DatabaseEntity entityAnnotation = mTypeElement.getAnnotation(DatabaseEntity.class);
        String primaryKey = entityAnnotation.primaryKey();

        boolean primaryKeySet = false;

        for(int i=0;i<fields.size();i++) {
            final Element field = fields.get(i);
            final String fieldName = getDatabaseFieldName(field);
            final String fieldType = getDatabaseType(field);
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
            throw new IllegalStateException("Primary key not found when creating SQL for entity " + mTypeElement.getSimpleName());

        builder.append(")");

        return builder.toString();
    }

    /**
     * Gets the serializer class from annotation. See http://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation for this weird hack.
     * @return type element of serializer class
     */
    protected TypeElement getSerializerType() {
        if(mSerializer == null) {
            try {
                DatabaseEntity entity = mTypeElement.getAnnotation(DatabaseEntity.class);
                entity.serializer();
            } catch (MirroredTypeException mte) {
                mSerializer = mTypeElementConverter.asTypeElement(mte.getTypeMirror());
            }
        }

        return mSerializer;
    }

    protected String getDatabaseType(Element field) {
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

                if(typeName.equals(TYPE_STRING)) {
                    return getNativeTypeForDatabase(field);
                }
                else {
                    return getDatabaseTypeFromMethodParameterInSerializer(getDeserializeMethodsInSerializer(), field);
                }
            default:
                throw new UnsupportedOperationException(field.getSimpleName() + " have a type not known by SlingerORM");
        }
    }

    protected String getNativeTypeForDatabase(Element field) {
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

                if(typeName.equals(TYPE_STRING)) {
                    return "TEXT";
                }
                else {
                    throw new UnsupportedOperationException("this should only be called for native types and strings!");
                }
            default:
                throw new IllegalStateException(field.getSimpleName() + " is unknown (getNativeTypeForDatabase(Element))");
        }
    }

    protected String getDatabaseTypeFromMethodParameterInSerializer(List<ExecutableElement> methods, Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");

        final ExecutableElement serializerMethod = getMethodInSerializerThatMatchesReturnTypeElement(methods, field);
        final TypeMirror typeMirror = serializerMethod.getReturnType();
        final TypeKind typeKind = typeMirror.getKind();
        if(typeKind != TypeKind.DECLARED) {
            throw new IllegalStateException("TypeKind should be declared but was " + typeKind);
        }

        final DeclaredType declaredType = (DeclaredType) typeMirror;
        final TypeElement typeElement = (TypeElement) declaredType.asElement();
        final String typeName = typeElement.getQualifiedName().toString();
        if(typeName.equals(TYPE_STRING)) {
            throw new UnsupportedOperationException("String type should not be passed to this method");
        }

        final List<? extends VariableElement> parameters = serializerMethod.getParameters();
        if(parameters.size() != 1) {
            throw new IllegalStateException("@DeserializeType/@SerializeType methods must have one parameter with a native type or String, got " + parameters.size() + " parameters for method " + serializerMethod.getSimpleName());
        }

        return getNativeTypeForDatabase(parameters.get(0));
    }

    protected List<ExecutableElement> getMethods() {
        if(mMethods == null) {
            mMethods = getMethodsInTypeElement(mTypeElement);
        }
        return mMethods;
    }

    protected List<Element> getFieldsUsedInDatabase() {
        if(mFields == null) {
            mFields = filter(getElementsInType(), new Predicate<Element>() {
                @Override
                public boolean test(Element item) {
                    return item.getKind() == ElementKind.FIELD && isDatabaseField(item);
                }
            });
        }

        return mFields;
    }

    protected ExecutableElement getMethodInSerializerThatMatchesReturnTypeElement(List<ExecutableElement> methods, Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");

        final String typeName = getDeclaredTypeName(field);
        final List<ExecutableElement> methodsWithCorrectReturnType = getMethodsWithReturnElement(methods, field);
        if(methodsWithCorrectReturnType.size() < 1) {
            throw new IllegalStateException("No @DeserializeType methods found with return type " + typeName);
        }

        if(methodsWithCorrectReturnType.size() > 1) {
            throw new IllegalStateException("Return value of @DeserializeType/@SerializeType methods must be unique, there are " + methodsWithCorrectReturnType.size() + " with return type " + typeName);
        }

        return methodsWithCorrectReturnType.get(0);
    }

    protected String getDeserializeMethodForFieldElement(Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");

        final ExecutableElement executableElement = getMethodInSerializerThatMatchesReturnTypeElement(getDeserializeMethodsInSerializer(), field);
        final List<? extends VariableElement> parameters = executableElement.getParameters();
        if(parameters.size() != 1) {
            throw new IllegalStateException("@DeserializeType methods must have one parameter, got " + parameters.size() + " parameters for method " + executableElement.getSimpleName());
        }

        final VariableElement parameterElement = parameters.get(0);
        final TypeMirror typeMirror = parameterElement.asType();
        final TypeKind typeKind = typeMirror.getKind();

        TypeElement typeElement = null;
        if(typeKind == TypeKind.DECLARED) {
            final DeclaredType declaredType = (DeclaredType) typeMirror;
            typeElement = (TypeElement) declaredType.asElement();
        }

        final ObjectType parameterObjectType = getObjectTypeForElement(typeKind, typeElement);
        if(parameterObjectType == ObjectType.OTHER)
            throw new IllegalArgumentException("declared type is not supported, only native and string types can be used as return value for a @DeserializeType method, method was " + executableElement.getSimpleName());

        final String valuePart = getValuePart(parameterObjectType, field);
        return "mSerializer." + executableElement.getSimpleName() + "(" + valuePart + ")";
    }

    protected List<ExecutableElement> getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(List<ExecutableElement> methods, final Element element) {
        if(methods == null) throw new IllegalArgumentException("methods is null");

        final TypeKind typeKind = getTypeKind(element);
        final ObjectType objectType = getObjectTypeForElement(typeKind, element);
        if(objectType != ObjectType.OTHER)
            throw new IllegalArgumentException("Element should be a declared type");

        final DeclaredType parameterDeclaredType = (DeclaredType) element.asType();
        final TypeElement parameterTypeElement = (TypeElement) parameterDeclaredType.asElement();
        final String elementTypeName = parameterTypeElement.getQualifiedName().toString();

        return filter(methods, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                final List<? extends VariableElement> parameters = item.getParameters();
                if (parameters.size() != 1) {
                    throw new IllegalStateException("@DeserializeType/@SerializeType methods must have one parameter, got " + parameters.size() + " parameters for method " + item.getSimpleName());
                }

                final VariableElement variableElement = parameters.get(0);
                final TypeKind parameterTypeKind = getTypeKind(variableElement);

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

    protected List<ExecutableElement> getMethodsWithReturnElement(final List<ExecutableElement> methods, final Element element) {
        final TypeKind elementTypeKind = getTypeKind(element);
        final String elementTypeName = getDeclaredTypeName(element);

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

    protected String getSetterWithParam(Element field) {
        String valuePart = getValuePart(getObjectTypeForElement(getTypeKind(field), field), field);
        Element method = findMethodByFieldNameAndSetFieldAnnotation(field.getSimpleName().toString());
        if(method != null)
            return method.getSimpleName() + "(" + valuePart + ")";

        method = findMethodByFieldNameOnly(getMethods(), field.getSimpleName().toString(), "set");
        if(method != null)
            return method.getSimpleName() + "(" + valuePart + ")";

        if (!isAccessable(field)) {
            throw new IllegalStateException("No get method or a public field for " + field.getSimpleName() + " in " + mTypeElement.getSimpleName());
        }

        return field.getSimpleName().toString() + "=" + valuePart;
    }

    protected Element findMethodByFieldNameAndSetFieldAnnotation(String fieldName) {
        for(ExecutableElement element : getMethods()) {
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

    protected String getIdGetter() {
        return findDirectGetter(getPrimaryKeyField());
    }

    protected Element getFieldByName(List<Element> fields, final String name) {
        return firstOrDefault(filter(fields, new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return name.equals(item.getSimpleName().toString());
            }
        }));
    }

    protected Element findSingleElementByAnnotation(List<Element> elements, final Class<? extends Annotation> annotationClass) {
        return firstOrDefault(filter(elements, new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getAnnotation(annotationClass) != null;
            }
        }));
    }

    protected String findDirectGetter(Element field) {
        ExecutableElement method = findMethodByFieldNameAndGetFieldAnnotation(getMethods(), field.getSimpleName().toString());
        if(method != null)
            return "item." + method.getSimpleName() + "()";

        boolean isBoolean = field.asType().getKind() == TypeKind.BOOLEAN;
        method = findMethodByFieldNameOnly(getMethods(), field.getSimpleName().toString(), isBoolean ? "is" : "get");
        if(method != null)
            return "item." + method.getSimpleName() + "()";

        if (!isAccessable(field))
            throw new IllegalStateException("No get method or a public field for " + field.getSimpleName() + " in " + mTypeElement.getSimpleName());

        return "item." + field.getSimpleName().toString();
    }

    protected String findGetterInSerializer(Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");
        final List<ExecutableElement> methods = getMethodsInSerializerThatMatchesParameterElementOfDeclaredType(getSerializeMethodsInSerializer(), field);

        if(methods.size() == 0) {
            throw new IllegalArgumentException("No method found for field " + field.asType());
        }

        if(methods.size() > 1) {
            throw new IllegalArgumentException("Only one @SerializeType method per parameter type supported, found " + methods.size() + " methods with same parameter type in " + field.asType());
        }

        return "mSerializer." + methods.get(0).getSimpleName() + "(" + findDirectGetter(field) + ")";
    }

    protected String findGetter(Element field) {
        final TypeKind fieldTypeKind = getTypeKind(field);
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
                return findGetterInSerializer(field);
            default:
                throw new IllegalStateException("this cannot be called?");
        }
    }

    protected ExecutableElement findMethodByFieldNameOnly(List<ExecutableElement> methods, final String fieldName, final String prefix) {
        return firstOrDefault(filter(methods, new Predicate<ExecutableElement>() {
            @Override
            public boolean test(ExecutableElement item) {
                String firstLetter = fieldName.substring(0, 1).toUpperCase();
                String methodName = prefix + firstLetter + fieldName.substring(1);

                return methodName.equals(item.getSimpleName().toString());
            }
        }));
    }

    protected ExecutableElement findMethodByFieldNameAndGetFieldAnnotation(List<ExecutableElement> elements, String fieldName) {
        for(ExecutableElement element : elements) {
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

    protected Element getPrimaryKeyField() {
        List<Element> fields = getFieldsUsedInDatabase();
        Element field = findSingleElementByAnnotation(fields, PrimaryKey.class);
        if (field == null)
            field = getPrimaryKeyFieldUsingDatabaseEntity(mTypeElement, fields);

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

    protected String getValuePart(ObjectType objectType, Element field) {
        if(objectType == null) throw new IllegalArgumentException("objectType is null");

        switch (objectType) {
            case BOOLEAN:
                return "values.getBoolean(\"" + getDatabaseFieldName(field) + "\")";
            case SHORT:
                return "values.getShort(\"" + getDatabaseFieldName(field) + "\")";
            case INT:
                return "values.getInt(\"" + getDatabaseFieldName(field) + "\")";
            case LONG:
                return "values.getLong(\"" + getDatabaseFieldName(field) + "\")";
            case FLOAT:
                return "values.getFloat(\"" + getDatabaseFieldName(field) + "\")";
            case DOUBLE:
                return "values.getDouble(\"" + getDatabaseFieldName(field) + "\")";
            case STRING:
                return "values.getString(\"" + getDatabaseFieldName(field) + "\")";
            case OTHER:
                if(field == null) throw new IllegalArgumentException("field is null");
                return getDeserializeMethodForFieldElement(field);
            default:
                throw new IllegalStateException("unsupported objectType " + objectType);
        }
    }

    protected List<ExecutableElement> getDeserializeMethodsInSerializer() {
        if(mDeserializeMethodsInSerializer == null) {
            mDeserializeMethodsInSerializer = filter(getMethodsInSerializer(), new Predicate<ExecutableElement>() {
                @Override
                public boolean test(ExecutableElement item) {
                    return item.getAnnotation(DeserializeType.class) != null;
                }
            });
        }

        return mDeserializeMethodsInSerializer;
    }

    protected List<ExecutableElement> getSerializeMethodsInSerializer() {
        if (mSerializeMethodsInSerializer == null) {
            mSerializeMethodsInSerializer = filter(getMethodsInSerializer(), new Predicate<ExecutableElement>() {
                @Override
                public boolean test(ExecutableElement item) {
                    return item.getAnnotation(SerializeType.class) != null;
                }
            });
        }

        return mSerializeMethodsInSerializer;
    }

    protected List<ExecutableElement> getMethodsInSerializer() {
        if(mMethodsInSerializer == null) {
            mMethodsInSerializer = getMethodsInTypeElement(mSerializer);
        }

        return mMethodsInSerializer;
    }

    protected List<Element> getElementsInType() {
        if(mElementsInType == null) {
            mElementsInType = getElementsInTypeElement(mTypeElement);
        }

        return mElementsInType;
    }

    protected boolean isDatabaseField(Element field) {
        final Set<Modifier> modifiers = field.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("STATIC".equals(name) || "TRANSIENT".equals(name) || field.getAnnotation(NotDatabaseField.class) != null) {
                return false;
            }
        }

        return true;
    }

    protected String getDatabaseFieldName(Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");

        FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
        if(fieldNameAnnotation == null)
            return field.getSimpleName().toString();

        String fieldName = fieldNameAnnotation.value();
        if(fieldName == null || fieldName.equals(""))
            throw new IllegalStateException("fieldName must not be null or empty!");

        return fieldName;
    }

    public ObjectType getObjectTypeForElement(TypeKind typeKind, Element element) {
        if(typeKind == null) throw new IllegalArgumentException("typeKind is null");

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
                if(element == null) throw new IllegalArgumentException("element is null");

                final DeclaredType declaredType = (DeclaredType) element.asType();
                final TypeElement typeElement = (TypeElement) declaredType.asElement();
                final String typeName = typeElement.getQualifiedName().toString();
                if(typeName.equals(TYPE_STRING)) {
                    return ObjectType.STRING;
                }
                else {
                    return ObjectType.OTHER;
                }
            default:
                throw new IllegalStateException(typeKind + " is not known by SlingerORM");
        }
    }
}
