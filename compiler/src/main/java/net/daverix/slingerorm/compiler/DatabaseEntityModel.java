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
import net.daverix.slingerorm.annotation.Serializer;
import net.daverix.slingerorm.annotation.SetField;
import net.daverix.slingerorm.compiler.mapping.Getter;
import net.daverix.slingerorm.compiler.mapping.cursor.DefaultCursorGetter;
import net.daverix.slingerorm.compiler.mapping.cursor.DeserializeGetter;
import net.daverix.slingerorm.compiler.mapping.cursor.FieldSetter;
import net.daverix.slingerorm.compiler.mapping.cursor.MethodSetter;
import net.daverix.slingerorm.compiler.mapping.values.FieldValueGetter;
import net.daverix.slingerorm.compiler.mapping.values.MethodValueGetter;
import net.daverix.slingerorm.compiler.mapping.values.SerializeGetter;

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
import javax.lang.model.util.Types;

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
import static net.daverix.slingerorm.compiler.TypeUtils.isTypeMirrorEqual;

class DatabaseEntityModel {
    private static final String DEFAULT_DATE_SERIALIZER = "net.daverix.slingerorm.serialization.DateSerializer";

    private final TypeElement databaseTypeElement;
    private final Types typeElementConverter;
    private final Elements typeElementProvider;
    private final PackageProvider packageProvider;

    // field name - field model
    private final Map<String, FieldModel> fieldModels = new HashMap<>();
    private final Map<String, CursorType> cursorTypes = new HashMap<>();
    private final Map<String, String> columnNames = new HashMap<>();

    // field type - model for database entity
    private final Map<TypeElement, DatabaseEntityModel> databaseEntityModels = new HashMap<>();

    // column name - column model
    private final Map<String, ColumnModel> columns = new HashMap<>();

    private final List<String> itemSqlArgColumns = new ArrayList<>();
    private final Set<String> primaryKeyFields = new HashSet<>();

    private final Map<String, TypeElement> fieldSerializers = new HashMap<>();
    private final Map<String, String> serializerFieldClassNames = new HashMap<>();
    private final Set<String> serializerQualifiedNames = new HashSet<>();
    private final Map<String, String> fieldSerializerNames = new HashMap<>();

    private String mapperPackageName;
    private String mapperClassName;
    private String databaseEntityClassName;
    private String tableName;
    private String createTableSql;
    private String itemSql;


    public DatabaseEntityModel(TypeElement databaseTypeElement,
                               Types typeElementConverter,
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
        findFieldsThatAreDatabaseEntities();

        initializeSubDatabaseEntityModels();

        findGetFieldMethods();
        findGetMethods();
        findGetFieldAccess();
        findSetFieldMethods();
        findSetMethods();
        findSetFieldAccess();

        findSerializers();
        verifySerializersMatchFields();

        generateColumns();
        generateFieldSetters();

        generateCreateTableSql();
        generateNamesForSerializers();
        generateItemSql();
    }

    private void findOnDeleteActions() {
        for (FieldModel model : fieldModels.values()) {
            Element element = model.getElement();
            OnDelete onDelete = element.getAnnotation(OnDelete.class);
            if(onDelete != null) {
                model.setForeignKeyDeleteAction(onDelete.value());
            }
        }
    }

    private void findOnUpdateActions() {
        for (FieldModel model : fieldModels.values()) {
            Element element = model.getElement();
            OnUpdate onUpdate = element.getAnnotation(OnUpdate.class);
            if(onUpdate != null) {
                model.setForeignKeyUpdateAction(onUpdate.value());
            }
        }
    }

    private void findFieldsThatAreDatabaseEntities() throws InvalidElementException {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);

            Element element = fieldModel.getElement();
            TypeMirror typeMirror = element.asType();
            if (typeMirror.getKind() != TypeKind.DECLARED)
                return;

            TypeElement typeElement = (TypeElement) typeElementConverter.asElement(typeMirror);
            if (typeElement == null) {
                throw new InvalidElementException("Cannot get type from field", element);
            }
            if (typeElement.getAnnotation(DatabaseEntity.class) != null) {
                DatabaseEntityModel databaseEntityModel = databaseEntityModels.get(typeElement);
                if(databaseEntityModel == null) {
                    databaseEntityModel = new DatabaseEntityModel(typeElement,
                            typeElementConverter,
                            typeElementProvider,
                            packageProvider);
                    databaseEntityModels.put(typeElement, databaseEntityModel);
                }

                fieldModel.setDatabaseEntityModel(databaseEntityModel);
            }
        }
    }

    private void initializeSubDatabaseEntityModels() throws InvalidElementException {
        for (DatabaseEntityModel databaseEntityModel : databaseEntityModels.values()) {
            if(databaseEntityModel != null) {
                databaseEntityModel.initialize();
            }
        }
    }

    private void findSetFieldMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            SetField setField = method.getAnnotation(SetField.class);
            if (setField != null) {
                String fieldName = setField.value();
                FieldModel fieldModel = fieldModels.get(fieldName);
                if (fieldModel == null) {
                    throw new InvalidElementException("@SetField does not point to a valid field", method);
                }

                List<? extends VariableElement> methodParameters = method.getParameters();
                if (methodParameters.size() != 1) {
                    throw new InvalidElementException("@SetField method must have exactly one parameter", method);
                }
                VariableElement parameterElement = methodParameters.get(0);
                if (!fieldModel.getElement().asType().equals(parameterElement.asType())) {
                    throw new InvalidElementException("@SetField method parameter doesn't have the same type as the field it points to", method);
                }

                fieldModel.setAnnotatedSetterMethod(method.getSimpleName().toString());
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
                FieldModel fieldModel = fieldModels.get(fieldName);
                if (fieldModel == null) {
                    continue;
                }

                List<? extends VariableElement> methodParameters = method.getParameters();
                if (methodParameters.size() != 1) {
                    throw new InvalidElementException("set method must have one parameter", method);
                }

                VariableElement parameterElement = methodParameters.get(0);
                if (!fieldModel.getElement().asType().equals(parameterElement.asType())) {
                    throw new InvalidElementException("set method parameter doesn't have the same type as the field it points to", method);
                }

                fieldModel.setSetterMethod(method.getSimpleName().toString());
            }
        }
    }

    private void findSetFieldAccess() throws InvalidElementException {
        for (FieldModel fieldModel : fieldModels.values()) {
            if (fieldModel.getAnnotatedSetterMethod() != null) {
                fieldModel.setSetterFieldAccess(FieldAccess.ANNOTATED_METHOD);
            } else if (fieldModel.getSetterMethod() != null) {
                fieldModel.setSetterFieldAccess(FieldAccess.STANDARD_METHOD);
            } else if (isAccessible(fieldModel.getElement())) {
                fieldModel.setSetterFieldAccess(FieldAccess.FIELD);
            } else {
                throw new InvalidElementException("No way to access field in class, consider adding a set method or make the field public. The set method might need a @SetField annotation", fieldModel.getElement());
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
                FieldModel fieldModel = fieldModels.get(fieldName);
                if (fieldModel == null) {
                    continue;
                }

                if (!fieldModel.getElement().asType().equals(method.getReturnType())) {
                    throw new InvalidElementException("get method does not return the same type as the field it points to", method);
                }

                fieldModel.setGetterMethod(method.getSimpleName().toString());
            } else if (simpleName.length() > 2 && "is".equals(simpleName.substring(0, 2))) {
                String fieldName = simpleName.substring(2, 3).toLowerCase() + simpleName.substring(3);
                FieldModel fieldModel = fieldModels.get(fieldName);
                if (fieldModel == null) {
                    continue;
                }

                if (fieldModel.getElement().asType().getKind() != TypeKind.BOOLEAN) {
                    throw new InvalidElementException("is method must return boolean", method);
                }

                if (method.getReturnType().getKind() != TypeKind.BOOLEAN) {
                    throw new InvalidElementException("is method must point to a boolean field", method);
                }

                fieldModel.setGetterMethod(method.getSimpleName().toString());
            }
        }
    }

    private void findGetFieldMethods() throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(databaseTypeElement);

        for (ExecutableElement method : methods) {
            GetField getField = method.getAnnotation(GetField.class);
            if (getField != null) {
                String fieldName = getField.value();
                FieldModel fieldModel = fieldModels.get(fieldName);
                if (fieldModel == null) {
                    throw new InvalidElementException("GetField does not point to a valid field", method);
                }

                if (!fieldModel.getElement().asType().equals(method.getReturnType())) {
                    throw new InvalidElementException("GetField method does not return the same type as the field it points to", method);
                }

                fieldModel.setAnnotatedGetterMethod(method.getSimpleName().toString());
            }
        }
    }

    private void findGetFieldAccess() throws InvalidElementException {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);
            if (fieldModel.getAnnotatedGetterMethod() != null) {
                fieldModel.setGetterFieldAccess(FieldAccess.ANNOTATED_METHOD);
            } else if (fieldModel.getGetterMethod() != null) {
                fieldModel.setGetterFieldAccess(FieldAccess.STANDARD_METHOD);
            } else if (isAccessible(fieldModel.getElement())) {
                fieldModel.setGetterFieldAccess(FieldAccess.FIELD);
            } else {
                throw new InvalidElementException("No way to access field in class, consider adding a get method or make the field public. The get method might need a @GetField annotation", fieldModel.getElement());
            }
        }
    }

    private void generateFieldSetters() throws InvalidElementException {
        for (FieldModel fieldModel : fieldModels.values()) {
            createFieldSetter(fieldModel);
        }
    }

    private void createFieldSetter(FieldModel fieldModel) throws InvalidElementException {
        switch (fieldModel.getGetterFieldAccess()) {
            case FIELD:
                fieldModel.setSetter(new FieldSetter(fieldModel.getName(), getGetterForFieldModel(fieldModel)));
                break;
            case STANDARD_METHOD:
                fieldModel.setSetter(new MethodSetter(fieldModel.getSetterMethod(), getGetterForFieldModel(fieldModel)));
                break;
            case ANNOTATED_METHOD:
                fieldModel.setSetter(new MethodSetter(fieldModel.getAnnotatedSetterMethod(), getGetterForFieldModel(fieldModel)));
                break;
            default:
                throw new InvalidElementException("Could not get access of field", fieldModel.getElement());
        }
    }

    private Getter getGetterForFieldModel(FieldModel fieldModel) {
        if(fieldModel.getSerializer() != null) {
            String serializerName = fieldSerializerNames.get(fieldModel.getName());

            return new DeserializeGetter(serializerName, createCursorGetter(fieldModel));
        }

        return createCursorGetter(fieldModel);
    }

    private DefaultCursorGetter createCursorGetter(FieldModel fieldModel) {
        return new DefaultCursorGetter(cursorTypes.get(fieldModel.getName()),
                columnNames.get(fieldModel.getName()));
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
            TypeElement enclosingTypeElement = (TypeElement) typeElementConverter.asElement(enclosingElement.asType());
            return getEntityClassName(enclosingTypeElement) + "." + typeElement.getSimpleName().toString();
        }

        return "";
    }

    private void findTableName() throws InvalidElementException {
        DatabaseEntity annotation = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String tableName = annotation.name();
        if (tableName.equals("")) {
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
            throw new InvalidElementException("Database Entity must have fieldModels in order to be stored", databaseTypeElement);

        for (Element field : fields) {
            this.fieldModels.put(field.getSimpleName().toString(),
                    new FieldModel(field
                    ));
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

        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);
            fieldModel.checkIsPrimaryKey();

            if (fieldModel.isPrimaryKey()) {
                primaryKeyFields.add(field);
            }
        }

        if (primaryKeyFields.size() == 0) {
            throw new InvalidElementException("No primary keys found in entity! Annotate fieldModels with @PrimaryKey or set the primary keys in the annotation", databaseTypeElement);
        }
    }

    private void findPrimaryKeyFieldsInDatabaseEntityAnnotation() throws InvalidElementException {
        DatabaseEntity databaseEntity = databaseTypeElement.getAnnotation(DatabaseEntity.class);
        String[] fields = databaseEntity.primaryKeyFields();

        if (fields.length == 0)
            return;

        for (String field : fields) {
            if (field == null || field.equals(""))
                continue;

            if (!fieldModels.containsKey(field)) {
                throw new InvalidElementException(String.format("Field \"%s\"specified in DatabaseEntity as a primary key can't be found in the class or it's super class", field), databaseTypeElement);
            }

            primaryKeyFields.add(field);
        }
    }

    private void findSerializers() {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);

            Element element = fieldModel.getElement();
            TypeElement serializer = null;
            if (element.getAnnotation(Serializer.class) != null) {
                serializer = getSerializerTypeElement(element);
            } else if (isDate(element)) {
                serializer = getDefaultDateSerializer();
            }

            fieldModel.setSerializer(serializer);
            fieldSerializers.put(field, serializer);
        }
    }

    private TypeElement getSerializerTypeElement(Element field) {
        Serializer serializer = field.getAnnotation(Serializer.class);
        try {
            serializer.value();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return (TypeElement) typeElementConverter.asElement(mte.getTypeMirror());
        }
    }

    private TypeElement getDefaultDateSerializer() {
        return typeElementProvider.getTypeElement(DEFAULT_DATE_SERIALIZER);
    }

    private void verifySerializersMatchFields() throws InvalidElementException {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);

            TypeElement serializer = fieldModel.getSerializer();
            if (serializer == null) return;

            ExecutableElement deserializeMethod = findMethodWithName("deserialize", serializer);
            if (deserializeMethod == null)
                throw new IllegalStateException("deserialize method not found, how?");

            Element element = fieldModel.getElement();
            if (!isTypeMirrorEqual(element.asType(), deserializeMethod.getReturnType()))
                throw new InvalidElementException(String.format("Serializer doesn't match the type of the field. Expected %s but got %s", element.asType(), deserializeMethod.getReturnType()), element);
        }
    }

    public ColumnDataType getDatabaseTypeForField(String field) throws InvalidElementException {
        FieldModel fieldModel = fieldModels.get(field);
        if(fieldModel == null)
            throw new IllegalArgumentException("database type could not be found for field " + field);

        TypeElement serializer = fieldModel.getSerializer();
        ColumnDataType databaseType;
        if (serializer != null) {
            databaseType = getDatabaseType(getSerializerDatabaseValueParameter(serializer));
        } else {
            databaseType = getDatabaseType(fieldModels.get(field).getElement());
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

    private void generateColumns() throws InvalidElementException {
        generateNonDatabaseColumns();
        generateDatabaseEntityColumns();
    }

    private void generateNonDatabaseColumns() throws InvalidElementException {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);
            if(fieldModel.isDatabaseEntity()) {
                continue;
            }

            CursorType cursorType = getCursorTypeForField(field);
            cursorTypes.put(field, cursorType);

            String columnName = findColumnName(fieldModel.getElement());
            columnNames.put(field, columnName);

            ColumnDataType dataType = getDatabaseTypeForField(field);
            boolean primaryKey = primaryKeyFields.contains(field);

            Getter getter = createColumnGetter(fieldModel);

            columns.put(columnName, new ColumnModel(columnName, dataType, primaryKey, getter));
        }
    }

    private Getter createColumnGetter(FieldModel fieldModel) {
        String fieldName = fieldModel.getName();
        TypeElement serializer = fieldSerializers.get(fieldName);
        if(serializer != null) {
            return new SerializeGetter(fieldSerializerNames.get(fieldName),
                    createValuesGetterForFieldModel(fieldModel));
        }

        return createValuesGetterForFieldModel(fieldModel);
    }

    private Getter createValuesGetterForFieldModel(FieldModel fieldModel) {
        switch (fieldModel.getGetterFieldAccess()) {
            case FIELD:
                return new FieldValueGetter(fieldModel.getName());
            case STANDARD_METHOD:
                return new MethodValueGetter(fieldModel.getGetterMethod());
            case ANNOTATED_METHOD:
                return new MethodValueGetter(fieldModel.getAnnotatedGetterMethod());
            default:
                return null;
        }
    }

    private void generateDatabaseEntityColumns() throws InvalidElementException {
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);
            generateColumnsFromPrimaryKeys(fieldModel);
        }
    }

    private void generateColumnsFromPrimaryKeys(FieldModel fieldModel) throws InvalidElementException {
        DatabaseEntityModel databaseEntityModel = fieldModel.getDatabaseEntityModel();
        if (databaseEntityModel == null)
            return;

        String firstPartOfColumnName = findColumnName(fieldModel.getElement());
        Set<String> dbEntityPrimaryKeys = databaseEntityModel.getPrimaryKeyFields();
        for (String dbEntityPrimaryKey : dbEntityPrimaryKeys) {
            String columnName = createColumnNameForSubEntity(firstPartOfColumnName, dbEntityPrimaryKey);
            if (columns.containsKey(columnName)) {
                throw new InvalidElementException("Could not use " + columnName + " for database entity as there is already a column with that name defined in the current class.", fieldModel.getElement());
            }

            ColumnModel primaryKeyModel = databaseEntityModel.getColumns().get(dbEntityPrimaryKey);
            if(primaryKeyModel == null)
                throw new IllegalStateException("could not find column model for primary key " + dbEntityPrimaryKey);

            columns.put(columnName, new ColumnModel(
                    columnName,
                    primaryKeyModel.getDataType(),
                    false,
                    createColumnGetter(fieldModel)));
        }
    }

    private String findColumnName(Element element) throws InvalidElementException {
        ColumnName columnNameAnnotation = element.getAnnotation(ColumnName.class);
        String columnName;
        if (columnNameAnnotation == null) {
            columnName = element.getSimpleName().toString();
        } else {
            columnName = columnNameAnnotation.value();
        }

        if (columnName.equals(""))
            throw new InvalidElementException("columnName must not be null or empty!", element);

        return columnName;
    }

    private String createColumnNameForSubEntity(String firstPart, String subEntityPrimaryKey) {
        return firstPart + "_" + subEntityPrimaryKey;
    }

    private CursorType getCursorTypeForField(String field) throws InvalidElementException {
        FieldModel fieldModel = fieldModels.get(field);
        if(fieldModel == null)
            throw new IllegalArgumentException("cursor type could not be found for field " + field);

        TypeElement serializer = fieldModel.getSerializer();
        CursorType cursorType;
        if (serializer != null) {
            cursorType = getCursorType(getSerializerDatabaseValueParameter(serializer));
        } else {
            cursorType = getCursorType(fieldModels.get(field).getElement());
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
        List<String> primaryKeyColumns = new ArrayList<>();
        for (String columnName : columns.keySet()) {
            final ColumnModel column = columns.get(columnName);
            if (column.isPrimaryKey()) {
                primaryKeyColumns.add(columnName);
            }
        }
        return String.join(", ", primaryKeyColumns);
    }

    private String createSqlFieldsText() {
        List<String> typeNamePairs = new ArrayList<>();
        for (String columnName : columns.keySet()) {
            ColumnModel model = columns.get(columnName);
            final String fieldType = model.getDataType().name();

            typeNamePairs.add(columnName + " " + fieldType);
        }
        return String.join(", ", typeNamePairs);
    }

    private String createSqlForeignKeysText() throws InvalidElementException {
        StringBuilder foreignKeys = new StringBuilder();
        for (String field : fieldModels.keySet()) {
            FieldModel fieldModel = fieldModels.get(field);

            DatabaseEntityModel databaseEntity = fieldModel.getDatabaseEntityModel();
            if(databaseEntity == null)
                continue;

            List<String> primaryKeyFields = new ArrayList<>();
            for(String dbEntityKey : databaseEntity.getPrimaryKeyFields()) {
                primaryKeyFields.add(createColumnNameForSubEntity(field, dbEntityKey));
            }

            foreignKeys.append(", FOREIGN KEY(")
                    .append(String.join(",", primaryKeyFields))
                    .append(") REFERENCES ").append(databaseEntity.getTableName()).append("(")
                    .append(String.join(",", databaseEntity.getPrimaryKeyFields()))
                    .append(")");

            ForeignKeyAction onUpdateAction = fieldModel.getForeignKeyUpdateAction();
            if(onUpdateAction != null) {
                foreignKeys.append(" ON UPDATE ").append(getStringFromAction(onUpdateAction));
            }

            ForeignKeyAction onDeleteAction = fieldModel.getForeignKeyDeleteAction();
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
        Set<TypeElement> uniqueSerializerTypes = new HashSet<>(fieldSerializers.values());
        Map<String,Integer> fieldNameOccurrences = new HashMap<>();

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
        for (String field : fieldSerializers.keySet()) {
            if (typeElement.equals(fieldSerializers.get(field))) {
                return field;
            }
        }
        return null;
    }

    private void generateItemSql() {
        List<String> expressions = new ArrayList<String>();
        for (String field : primaryKeyFields) {
            expressions.add(columns.get(field).getName() + "=?");
            itemSqlArgColumns.add(field);
        }
        itemSql = String.join(" AND ", expressions);
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, FieldModel> getFields() {
        return fieldModels;
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

    public List<String> getItemSqlArgColumns() {
        return itemSqlArgColumns;
    }

    public boolean hasSerializer(String field) {
        return fieldSerializers.containsKey(field);
    }

    public String getSerializerFieldName(String field) {
        return fieldSerializerNames.get(field);
    }

    public Set<String> getPrimaryKeyFields() {
        return primaryKeyFields;
    }

    public boolean isFieldString(String field) {
        return isString(fieldModels.get(field).getElement());
    }

    public Map<String, ColumnModel> getColumns() {
        return columns;
    }
}
