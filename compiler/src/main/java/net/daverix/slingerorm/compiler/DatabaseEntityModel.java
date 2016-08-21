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

import net.daverix.slingerorm.annotation.ColumnName;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.ForeignKeyAction;
import net.daverix.slingerorm.annotation.GetField;
import net.daverix.slingerorm.annotation.NotDatabaseField;
import net.daverix.slingerorm.annotation.OnDelete;
import net.daverix.slingerorm.annotation.OnUpdate;
import net.daverix.slingerorm.annotation.PrimaryKey;
import net.daverix.slingerorm.annotation.Serializer;
import net.daverix.slingerorm.annotation.SetField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_BOOLEAN;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_DOUBLE;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_FLOAT;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_INTEGER;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_LONG;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_SHORT;
import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_STRING;
import static net.daverix.slingerorm.compiler.ElementUtils.findMethodWithName;
import static net.daverix.slingerorm.compiler.ElementUtils.getElementsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.getMethodsInTypeElement;
import static net.daverix.slingerorm.compiler.ElementUtils.isAccessible;
import static net.daverix.slingerorm.compiler.ElementUtils.isDate;
import static net.daverix.slingerorm.compiler.ElementUtils.isString;
import static net.daverix.slingerorm.compiler.ListUtils.filter;
import static net.daverix.slingerorm.compiler.StringUtils.lowerCaseFirstCharacter;

class DatabaseEntityModel {
    public static final String DEFAULT_DATE_SERIALIZER = "net.daverix.slingerorm.serialization.DateSerializer";
    private final TypeElement databaseTypeElement;
    private final TypeElementConverter typeElementConverter;
    private final Elements typeElementProvider;
    private final PackageProvider packageProvider;

    private final Map<String, Element> fieldsInUse = new HashMap<String, Element>();
    private final Map<String, TypeElement> fieldSerializer = new HashMap<String, TypeElement>();
    private final Map<String, Integer> fieldNameOccurrences = new HashMap<String, Integer>();
    private final Map<String, String> getMethods = new HashMap<String, String>();
    private final Map<String, String> annotatedGetMethods = new HashMap<String, String>();
    private final Map<String, String> setMethods = new HashMap<String, String>();
    private final Map<String, String> annotatedSetMethods = new HashMap<String, String>();
    private final List<String> itemSqlArgFields = new ArrayList<String>();
    private final Set<String> primaryKeyFields = new HashSet<String>();
    private final Map<String, FieldAccess> fieldGetAccess = new HashMap<String, FieldAccess>();
    private final Map<String, FieldAccess> fieldSetAccess = new HashMap<String, FieldAccess>();
    private final Map<String, String> serializerFieldClassNames = new HashMap<String, String>();
    private final Map<String, ColumnDataType> fieldDatabaseTypes = new HashMap<String, ColumnDataType>();
    private final Map<String, CursorType> cursorTypes = new HashMap<String, CursorType>();
    private final Map<String, String> columnNames = new HashMap<String, String>();
    private final Set<String> serializerQualifiedNames = new HashSet<String>();
    private final Map<String, String> fieldSerializerNames = new HashMap<String, String>();
    private final Map<String, DatabaseEntityModel> subModels = new HashMap<String, DatabaseEntityModel>();
    private final Map<String, ForeignKeyAction> foreignKeyOnUpdate = new HashMap<>();
    private final Map<String, ForeignKeyAction> foreignKeyOnDelete = new HashMap<>();

    private String mapperPackageName;
    private String mapperClassName;
    private String databaseEntityClassName;
    private String tableName;
    private String createTableSql;
    private String itemSql;

    public DatabaseEntityModel(TypeElement databaseTypeElement,
                               TypeElementConverter typeElementConverter,
                               Elements typeElementProvider,
                               PackageProvider packageProvider) {
        this.databaseTypeElement = databaseTypeElement;
        this.typeElementConverter = typeElementConverter;
        this.typeElementProvider = typeElementProvider;
        this.packageProvider = packageProvider;
    }

    public void initialize() throws InvalidElementException {
        findMapperPackageName();
        findMapperClassName();
        findEntityClassName();
        findTableName();
        findFieldsInUse();
        findPrimaryKeyFields();
        findOnUpdateActions();
        findOnDeleteActions();
        findSubModels();
        findColumnNames();

        initializeSubModels();

        findGetFieldMethods();
        findGetMethods();
        findGetFieldAccess();
        findSetFieldMethods();
        findSetMethods();
        findSetFieldAccess();

        findSerializers();
        verifySerializersMatchFields();

        findDatabaseTypesForFields();
        findCursorTypesForFields();
        generateCreateTableSql();
        generateNamesForSerializers();
        generateItemSql();
    }

    private void findOnDeleteActions() {
        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);
            OnDelete onDelete = element.getAnnotation(OnDelete.class);
            if(onDelete != null) {
                foreignKeyOnDelete.put(field, onDelete.value());
            }
        }
    }

    private void findOnUpdateActions() {
        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);
            OnUpdate onUpdate = element.getAnnotation(OnUpdate.class);
            if(onUpdate != null) {
                foreignKeyOnUpdate.put(field, onUpdate.value());
            }
        }
    }

    private void findSubModels() throws InvalidElementException {
        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);
            TypeMirror typeMirror = element.asType();
            if (typeMirror.getKind() != TypeKind.DECLARED)
                continue;

            TypeElement typeElement = typeElementConverter.asTypeElement(typeMirror);
            if (typeElement == null) {
                throw new InvalidElementException("Cannot get type from field", element);
            }
            if (typeElement.getAnnotation(DatabaseEntity.class) != null) {
                subModels.put(field, new DatabaseEntityModel(typeElement,
                        typeElementConverter,
                        typeElementProvider,
                        packageProvider));
            }
        }
    }

    private void initializeSubModels() throws InvalidElementException {
        for (DatabaseEntityModel model : subModels.values()) {
            model.initialize();
        }
    }

    private void findSetFieldMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            SetField setField = method.getAnnotation(SetField.class);
            if (setField != null) {
                String fieldName = setField.value();
                Element fieldElement = fieldsInUse.get(fieldName);
                if (fieldElement == null) {
                    throw new InvalidElementException("@SetField does not point to a valid field", method);
                }

                List<? extends VariableElement> methodParameters = method.getParameters();
                if (methodParameters.size() != 1) {
                    throw new InvalidElementException("@SetField method must have exactly one parameter", method);
                }
                VariableElement parameterElement = methodParameters.get(0);
                if (!fieldElement.asType().equals(parameterElement.asType())) {
                    throw new InvalidElementException("@SetField method parameter doesn't have the same type as the field it points to", method);
                }

                annotatedSetMethods.put(fieldName, method.getSimpleName().toString());
            }
        }
    }

    private void findSetMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            if (method.getAnnotation(SetField.class) != null) continue;

            String simpleName = method.getSimpleName().toString();
            if (simpleName.length() > 3 && "set".equals(simpleName.substring(0, 3))) {
                String fieldName = simpleName.substring(3, 4).toLowerCase() + simpleName.substring(4);
                Element fieldElement = fieldsInUse.get(fieldName);
                if (fieldElement == null) {
                    continue;
                }

                List<? extends VariableElement> methodParameters = method.getParameters();
                if (methodParameters.size() != 1) {
                    throw new InvalidElementException("set method must have one parameter", method);
                }

                VariableElement parameterElement = methodParameters.get(0);
                if (!fieldElement.asType().equals(parameterElement.asType())) {
                    throw new InvalidElementException("set method parameter doesn't have the same type as the field it points to", method);
                }

                setMethods.put(fieldName, method.getSimpleName().toString());
            }
        }
    }

    private void findSetFieldAccess() throws InvalidElementException {
        for (String field : fieldsInUse.keySet()) {
            if (annotatedSetMethods.containsKey(field)) {
                fieldSetAccess.put(field, FieldAccess.ANNOTATED_METHOD);
            } else if (setMethods.containsKey(field)) {
                fieldSetAccess.put(field, FieldAccess.STANDARD_METHOD);
            } else if (isAccessible(fieldsInUse.get(field))) {
                fieldSetAccess.put(field, FieldAccess.FIELD);
            } else {
                throw new InvalidElementException("No way to access field in class, consider adding a set method or make the field public. The set method might need a @SetField annotation", fieldsInUse.get(field));
            }
        }
    }

    private void findGetMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            if (method.getAnnotation(GetField.class) != null) continue;

            String simpleName = method.getSimpleName().toString();
            if (simpleName.length() > 3 && "get".equals(simpleName.substring(0, 3))) {
                String fieldName = simpleName.substring(3, 4).toLowerCase() + simpleName.substring(4);
                Element fieldElement = fieldsInUse.get(fieldName);
                if (fieldElement == null) {
                    continue;
                }

                if (!fieldElement.asType().equals(method.getReturnType())) {
                    throw new InvalidElementException("get method does not return the same type as the field it points to", method);
                }

                getMethods.put(fieldName, method.getSimpleName().toString());
            } else if (simpleName.length() > 2 && "is".equals(simpleName.substring(0, 2))) {
                String fieldName = simpleName.substring(2, 3).toLowerCase() + simpleName.substring(3);
                Element fieldElement = fieldsInUse.get(fieldName);
                if (fieldElement == null) {
                    continue;
                }

                if (fieldElement.asType().getKind() != TypeKind.BOOLEAN) {
                    throw new InvalidElementException("is method must return boolean", method);
                }

                if (method.getReturnType().getKind() != TypeKind.BOOLEAN) {
                    throw new InvalidElementException("is method must point to a boolean field", method);
                }

                getMethods.put(fieldName, method.getSimpleName().toString());
            }
        }
    }


    private void findGetFieldMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            GetField getField = method.getAnnotation(GetField.class);
            if (getField != null) {
                String fieldName = getField.value();
                Element fieldElement = fieldsInUse.get(fieldName);
                if (fieldElement == null) {
                    throw new InvalidElementException("GetField does not point to a valid field", method);
                }

                if (!fieldElement.asType().equals(method.getReturnType())) {
                    throw new InvalidElementException("GetField method does not return the same type as the field it points to", method);
                }

                annotatedGetMethods.put(fieldName, method.getSimpleName().toString());
            }
        }
    }

    private void findGetFieldAccess() throws InvalidElementException {
        for (String field : fieldsInUse.keySet()) {
            if (annotatedGetMethods.containsKey(field)) {
                fieldGetAccess.put(field, FieldAccess.ANNOTATED_METHOD);
            } else if (getMethods.containsKey(field)) {
                fieldGetAccess.put(field, FieldAccess.STANDARD_METHOD);
            } else if (isAccessible(fieldsInUse.get(field))) {
                fieldGetAccess.put(field, FieldAccess.FIELD);
            } else {
                throw new InvalidElementException("No way to access field in class, consider adding a get method or make the field public. The get method might need a @GetField annotation", fieldsInUse.get(field));
            }
        }
    }

    private void findMapperPackageName() {
        mapperPackageName = typeElementProvider.getPackageOf(databaseTypeElement).getQualifiedName().toString();
    }

    private void findMapperClassName() {
        mapperClassName = databaseTypeElement.getSimpleName() + "Mapper";
    }

    private void findEntityClassName() throws InvalidElementException {
        databaseEntityClassName = getEntityClassName(databaseTypeElement);
    }

    private String getEntityClassName(TypeElement typeElement) throws InvalidElementException {
        if(typeElement.getNestingKind() == NestingKind.TOP_LEVEL) {
            return typeElement.getSimpleName().toString();
        }

        if(!typeElement.getModifiers().contains(Modifier.STATIC)) {
            throw new InvalidElementException("Class must be static in order for SlingerORM to be able to instantiate it", typeElement);
        }

        Element enclosingElement = typeElement.getEnclosingElement();
        if(enclosingElement.getKind() == ElementKind.CLASS) {
            TypeElement enclosingTypeElement = typeElementConverter.asTypeElement(enclosingElement.asType());
            return getEntityClassName(enclosingTypeElement) + "." + typeElement.getSimpleName().toString();
        }

        return "";
    }

    private void findTableName() throws InvalidElementException {
        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String tableName = annotation.name();
        if (tableName == null || tableName.equals("")) {
            this.tableName = databaseTypeElement.getSimpleName().toString();
        } else {
            this.tableName = tableName;
        }
    }

    private void findFieldsInUse() throws InvalidElementException {
        List<Element> fields = filter(getElementsInTypeElement(databaseTypeElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.FIELD && isDatabaseField(item);
            }
        });

        if (fields.size() == 0)
            throw new InvalidElementException("Database Entity must have fields in order to be stored", databaseTypeElement);

        for (Element field : fields) {
            fieldsInUse.put(field.getSimpleName().toString(), field);
        }
    }

    private boolean isDatabaseField(Element field) {
        if (field == null) throw new IllegalArgumentException("field is null");

        final Set<Modifier> modifiers = field.getModifiers();

        if (modifiers.contains(Modifier.STATIC))
            return false;

        if (modifiers.contains(Modifier.TRANSIENT))
            return false;

        if (field.getAnnotation(NotDatabaseField.class) != null)
            return false;

        return true;
    }

    private void findPrimaryKeyFields() throws InvalidElementException {
        findPrimaryKeyFieldsInDatabaseEntityAnnotation();

        if (primaryKeyFields.size() > 0) return;

        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);
            if (element.getAnnotation(PrimaryKey.class) != null) {
                primaryKeyFields.add(field);
            }
        }

        if (primaryKeyFields.size() == 0) {
            throw new InvalidElementException("No primary keys found in entity! Annotate fields with @PrimaryKey or set the primary keys in the annotation", databaseTypeElement);
        }
    }

    private void findPrimaryKeyFieldsInDatabaseEntityAnnotation() throws InvalidElementException {
        DatabaseEntity databaseEntity = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String[] fields = databaseEntity.primaryKeyFields();

        if (fields == null || fields.length == 0)
            return;

        for (String field : fields) {
            if (field == null || field.equals(""))
                continue;

            if (!fieldsInUse.containsKey(field)) {
                throw new InvalidElementException(String.format("Field \"%s\"specified in DatabaseEntity as a primary key can't be found in the class or it's super class", field), databaseTypeElement);
            }

            primaryKeyFields.add(field);
        }
    }

    private void findColumnNames() throws InvalidElementException {
        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);

            ColumnName columnNameAnnotation = element.getAnnotation(ColumnName.class);
            String columnName;
            if (columnNameAnnotation == null) {
                columnName = element.getSimpleName().toString();
            } else {
                columnName = columnNameAnnotation.value();
            }

            if (columnName == null || columnName.equals(""))
                throw new InvalidElementException("columnName must not be null or empty!", element);

            columnNames.put(field, columnName);
        }
    }

    private void findSerializers() {
        for (String field : fieldsInUse.keySet()) {
            Element element = fieldsInUse.get(field);
            if (element.getAnnotation(Serializer.class) != null) {
                fieldSerializer.put(field, getSerializerTypeElement(element));

            } else if (isDate(element)) {
                fieldSerializer.put(field, getDefaultDateSerializer());
            }
        }
    }

    private TypeElement getSerializerTypeElement(Element field) {
        Serializer serializer = field.getAnnotation(Serializer.class);
        try {
            serializer.value();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return typeElementConverter.asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement getDefaultDateSerializer() {
        return typeElementProvider.getTypeElement(DEFAULT_DATE_SERIALIZER);
    }

    private void verifySerializersMatchFields() throws InvalidElementException {
        for (String field : fieldsInUse.keySet()) {
            TypeElement serializerType = fieldSerializer.get(field);
            if (serializerType == null) continue;

            ExecutableElement deserializeMethod = findMethodWithName("deserialize", serializerType);
            if (deserializeMethod == null)
                throw new IllegalStateException("deserialize method not found, how?");

            Element fieldElement = fieldsInUse.get(field);
            if (!isTypeMirrorEqual(fieldElement.asType(), deserializeMethod.getReturnType()))
                throw new InvalidElementException(String.format("Serializer doesn't match the type of the field. Expected %s but got %s", fieldElement.asType(), deserializeMethod.getReturnType()), fieldElement);
        }
    }

    private boolean isTypeMirrorEqual(TypeMirror typeMirror, TypeMirror other) {
        if (typeMirror == other) return true;

        if (typeMirror.equals(other)) return true;

        if (typeMirror.getKind() == typeMirror.getKind() && typeMirror.getKind().isPrimitive())
            return true;

        if (typeMirror.getKind() == typeMirror.getKind() && typeMirror.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
            TypeElement otherTypeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();

            return typeElement.equals(otherTypeElement);
        }

        return false;
    }

    private void findDatabaseTypesForFields() throws InvalidElementException {
        for (String field : columnNames.keySet()) {
            ColumnDataType databaseType = getDatabaseTypeForField(field);

            fieldDatabaseTypes.put(field, databaseType);
        }
    }

    public ColumnDataType getDatabaseTypeForField(String field) throws InvalidElementException {
        TypeElement serializer = fieldSerializer.get(field);
        ColumnDataType databaseType;
        if (serializer != null) {
            databaseType = getDatabaseType(getSerializerDatabaseValueParameter(serializer));
        } else if (subModels.containsKey(field)) {
            DatabaseEntityModel databaseEntityModel = subModels.get(field);
            Set<String> primaryKeyFields = databaseEntityModel.getPrimaryKeyFields();
            if (primaryKeyFields.size() != 1) {
                throw new InvalidElementException("Only one primary key is supported when adding DatabaseEntity types", fieldsInUse.get(field));
            }

            //TODO: add support for multiple keys by specifying them in an annotation?
            String primaryKeyField = new ArrayList<>(primaryKeyFields).get(0);
            databaseType = databaseEntityModel.getDatabaseTypeForField(primaryKeyField);
        } else {
            databaseType = getDatabaseType(fieldsInUse.get(field));
        }
        return databaseType;
    }

    private TypeElement getSerializerDatabaseValueParameter(TypeElement serializerType) throws InvalidElementException {
        ExecutableElement deserializeMethod = findMethodWithName("serialize", serializerType);
        if (deserializeMethod == null)
            throw new IllegalStateException("serialize method not found, how?");

        return (TypeElement) ((DeclaredType) deserializeMethod.getReturnType()).asElement();
    }

    private ColumnDataType getDatabaseType(Element element) throws InvalidElementException {
        if (element == null) throw new IllegalArgumentException("element is null");

        TypeMirror fieldType = element.asType();
        TypeKind typeKind = fieldType.getKind();
        switch (typeKind) {
            case BOOLEAN:
            case SHORT:
            case LONG:
            case INT:
                return ColumnDataType.INTEGER;
            case FLOAT:
            case DOUBLE:
                return ColumnDataType.REAL;
            case ARRAY:
                ArrayType arrayType = (ArrayType) fieldType;
                TypeMirror componentType = arrayType.getComponentType();
                if (componentType.getKind() == TypeKind.BYTE) {
                    return ColumnDataType.BLOB;
                } else {
                    throw new InvalidElementException(String.format("Array of type %s is unknown, use a data type supported by SQLite or create a custom serializer", componentType), element);
                }
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) fieldType;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                String typeName = typeElement.getQualifiedName().toString();

                if (typeName.equals(TYPE_STRING)) {
                    return ColumnDataType.TEXT;
                } else if (typeName.equals(TYPE_LONG) ||
                        typeName.equals(TYPE_INTEGER) ||
                        typeName.equals(TYPE_SHORT)) {
                    return ColumnDataType.INTEGER;
                } else if (typeName.equals(TYPE_DOUBLE) ||
                        typeName.equals(TYPE_FLOAT)) {
                    return ColumnDataType.REAL;
                } else {
                    throw new InvalidElementException(String.format("Type %s is unknown, use a data type supported by SQLite or create a custom serializer", typeElement), element);
                }
            default:
                throw new InvalidElementException("Type not known by SlingerORM, solve this by creating a custom serializer", element);
        }
    }

    private void findCursorTypesForFields() throws InvalidElementException {
        for (String field : columnNames.keySet()) {
            CursorType cursorType = getCursorTypeForField(field);

            cursorTypes.put(field, cursorType);
        }
    }

    public CursorType getCursorTypeForField(String field) throws InvalidElementException {
        TypeElement serializer = fieldSerializer.get(field);
        CursorType cursorType;
        if (serializer != null) {
            cursorType = getCursorType(getSerializerDatabaseValueParameter(serializer));
        } else if (subModels.containsKey(field)) {
            DatabaseEntityModel databaseEntityModel = subModels.get(field);
            Set<String> primaryKeyFields = databaseEntityModel.getPrimaryKeyFields();
            if (primaryKeyFields.size() != 1) {
                throw new InvalidElementException("Only one primary key is supported when adding DatabaseEntity types", fieldsInUse.get(field));
            }

            //TODO: add support for multiple keys by specifying them in an annotation?
            String primaryKeyField = new ArrayList<String>(primaryKeyFields).get(0);
            cursorType = databaseEntityModel.getCursorTypeForField(primaryKeyField);
        } else {
            cursorType = getCursorType(fieldsInUse.get(field));
        }
        return cursorType;
    }

    private CursorType getCursorType(Element element) throws InvalidElementException {
        if (element == null) throw new IllegalArgumentException("element is null");

        TypeMirror fieldType = element.asType();
        TypeKind typeKind = fieldType.getKind();
        switch (typeKind) {
            case BOOLEAN:
                return CursorType.BOOLEAN;
            case SHORT:
                return CursorType.SHORT;
            case LONG:
                return CursorType.LONG;
            case INT:
                return CursorType.INT;
            case FLOAT:
                return CursorType.FLOAT;
            case DOUBLE:
                return CursorType.DOUBLE;
            case ARRAY:
                ArrayType arrayType = (ArrayType) fieldType;
                TypeMirror componentType = arrayType.getComponentType();
                if (componentType.getKind() == TypeKind.BYTE) {
                    return CursorType.BYTE_ARRAY;
                } else {
                    throw new InvalidElementException(String.format("Array of type %s can't be provided by an android cursor, use a custom serializer to solve this", componentType), element);
                }
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) fieldType;
                return getCursorType(element, declaredType);
            default:
                throw new InvalidElementException("Type not known by SlingerORM, solve this by creating a custom serializer", element);
        }
    }

    private CursorType getCursorType(Element element, DeclaredType declaredType) throws InvalidElementException {
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String typeName = typeElement.getQualifiedName().toString();

        switch (typeName) {
            case TYPE_STRING:
                return CursorType.STRING;
            case TYPE_LONG:
                return CursorType.LONG;
            case TYPE_INTEGER:
                return CursorType.INT;
            case TYPE_SHORT:
                return CursorType.SHORT;
            case TYPE_DOUBLE:
                return CursorType.DOUBLE;
            case TYPE_FLOAT:
                return CursorType.FLOAT;
            case TYPE_BOOLEAN:
                return CursorType.BOOLEAN;
            default:
                throw new InvalidElementException(String.format("Type %s can't be provided by an android cursor, use a custom serializer to solve this", typeElement), element);
        }
    }

    private void generateCreateTableSql() throws InvalidElementException {
        createTableSql = String.format("CREATE TABLE IF NOT EXISTS %s(%s, PRIMARY KEY(%s)%s)",
                getTableName(),
                createSqlFieldsText(),
                createSqlPrimaryKeysText(),
                createSqlForeignKeysText());
    }

    private String createSqlPrimaryKeysText() {
        List<String> primaryKeyColumns = new ArrayList<String>();
        for (String field : fieldsInUse.keySet()) {
            final String columnName = columnNames.get(field);
            if (primaryKeyFields.contains(field)) {
                primaryKeyColumns.add(columnName);
            }
        }
        return String.join(", ", primaryKeyColumns);
    }

    private String createSqlFieldsText() {
        List<String> typeNamePairs = new ArrayList<String>();
        for (String field : fieldsInUse.keySet()) {
            final String columnName = columnNames.get(field);
            final String fieldType = fieldDatabaseTypes.get(field).name();

            typeNamePairs.add(columnName + " " + fieldType);
        }
        return String.join(", ", typeNamePairs);
    }

    private String createSqlForeignKeysText() throws InvalidElementException {
        StringBuilder foreignKeys = new StringBuilder();
        for (String field : subModels.keySet()) {
            final String columnName = columnNames.get(field);

            //TODO: support multiple primary keys
            DatabaseEntityModel databaseEntity = getDatabaseEntity(field);
            List<String> primaryKeyFields = new ArrayList<>(databaseEntity.getPrimaryKeyFields());
            if(primaryKeyFields.size() < 1)
                throw new InvalidElementException("The field points to a database entity that doesn't have a primary key", fieldsInUse.get(field));

            if(primaryKeyFields.size() > 1)
                throw new InvalidElementException("The field points to a database entity which has more than one primary key which currently not supported.", fieldsInUse.get(field));

            foreignKeys.append(", FOREIGN KEY(").append(columnName).append(") REFERENCES ")
                    .append(databaseEntity.getTableName())
                    .append("(").append(primaryKeyFields.get(0)).append(")");

            ForeignKeyAction onUpdateAction = foreignKeyOnUpdate.get(field);
            if(onUpdateAction != null) {
                foreignKeys.append(" ON UPDATE ").append(getStringFromAction(onUpdateAction));
            }

            ForeignKeyAction onDeleteAction = foreignKeyOnDelete.get(field);
            if(onDeleteAction != null) {
                foreignKeys.append(" ON DELETE ").append(getStringFromAction(onDeleteAction));
            }
        }
        return foreignKeys.toString();
    }

    private static String getStringFromAction(ForeignKeyAction action) {
        switch (action) {
            case NO_ACTION:
                return "NO ACTION";
            case RESTRICT:
                return "RESTRICT";
            case SET_NULL:
                return "SET NULL";
            case SET_DEFAULT:
                return "SET DEFAULT";
            case CASCADE:
                return "CASCADE";
            default:
                return "INVALID";
        }
    }

    private void generateNamesForSerializers() {
        Set<TypeElement> uniqueSerializerTypes = new HashSet<TypeElement>(fieldSerializer.values());

        for (TypeElement serializerTypeElement : uniqueSerializerTypes) {
            String simpleName = serializerTypeElement.getSimpleName().toString();
            Integer occurrences = fieldNameOccurrences.get(simpleName);
            String serializerFieldName;
            if (occurrences == null || occurrences == 0) {
                serializerFieldName = lowerCaseFirstCharacter(simpleName);
                fieldNameOccurrences.put(simpleName, 1);
            } else {
                serializerFieldName = lowerCaseFirstCharacter(simpleName) + occurrences + 1;
                fieldNameOccurrences.put(simpleName, occurrences + 1);
            }

            serializerFieldClassNames.put(serializerFieldName, serializerTypeElement.getSimpleName().toString());
            serializerQualifiedNames.add(serializerTypeElement.getQualifiedName().toString());
            fieldSerializerNames.put(findFieldNameForTypeElement(serializerTypeElement), serializerFieldName);
        }
    }

    private String findFieldNameForTypeElement(TypeElement typeElement) {
        for (String field : fieldSerializer.keySet()) {
            if (typeElement.equals(fieldSerializer.get(field))) {
                return field;
            }
        }
        return null;
    }

    private void generateItemSql() {
        List<String> expressions = new ArrayList<String>();
        for (String field : primaryKeyFields) {
            expressions.add(columnNames.get(field) + "=?");
            itemSqlArgFields.add(field);
        }
        itemSql = String.join(" AND ", expressions);
    }

    public String getTableName() {
        return tableName;
    }

    public Set<String> getFieldNames() {
        return fieldsInUse.keySet();
    }

    public Collection<String> getColumnNames() {
        return columnNames.values();
    }

    public String getMapperPackageName() {
        return mapperPackageName;
    }

    public String getMapperClassName() {
        return mapperClassName;
    }

    public String getDatabaseEntityClassName() {
        return databaseEntityClassName;
    }

    public String getCreateTableSql() {
        return createTableSql;
    }

    public Collection<String> getSerializerFieldNames() {
        return serializerFieldClassNames.keySet();
    }

    public String getSerializerClassName(String serializerField) {
        return serializerFieldClassNames.get(serializerField);
    }

    public Collection<String> getQualifiedNamesForSerializers() {
        return serializerQualifiedNames;
    }

    public String getItemSql() {
        return itemSql;
    }

    public List<String> getItemSqlArgFields() {
        return itemSqlArgFields;
    }

    public FieldAccess getGetFieldAccess(String field) {
        return fieldGetAccess.get(field);
    }

    public FieldAccess getSetFieldAccess(String field) {
        return fieldSetAccess.get(field);
    }

    public boolean hasSerializer(String field) {
        return fieldSerializer.containsKey(field);
    }

    public String getColumnName(String field) {
        return columnNames.get(field);
    }

    public String getSerializerFieldName(String field) {
        return fieldSerializerNames.get(field);
    }

    public String getGetFieldAnnotatedMethod(String field) {
        return annotatedGetMethods.get(field);
    }

    public String getStandardGetMethod(String field) {
        return getMethods.get(field);
    }

    public CursorType getCursorType(String field) {
        return cursorTypes.get(field);
    }

    public String getSetFieldAnnotatedMethod(String field) {
        return annotatedSetMethods.get(field);
    }

    public String getStandardSetMethod(String field) {
        return setMethods.get(field);
    }

    public Set<String> getPrimaryKeyFields() {
        return primaryKeyFields;
    }

    public boolean isFieldString(String field) {
        return isString(fieldsInUse.get(field));
    }

    public boolean isDatabaseEntity(String field) {
        return subModels.containsKey(field);
    }

    public DatabaseEntityModel getDatabaseEntity(String field) {
        return subModels.get(field);
    }
}
