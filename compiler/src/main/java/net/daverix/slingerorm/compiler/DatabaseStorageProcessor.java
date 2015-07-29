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

import com.google.auto.service.AutoService;

import net.daverix.slingerorm.annotation.CreateTable;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.DatabaseStorage;
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;
import net.daverix.slingerorm.serialization.DefaultSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * This Processor creates Implementations of interfaces annotated with {@link DatabaseStorage}
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "net.daverix.slingerorm.annotation.DatabaseStorage"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseStorageProcessor extends AbstractProcessor {
    private static final String QUALIFIED_NAME_SQLITE_DATABASE = "android.database.sqlite.SQLiteDatabase";
    private static final List<String> SUPPORTED_RETURN_TYPES_FOR_SELECT = Arrays.asList(
            "java.util.List",
            "java.util.Collection",
            "java.lang.Iterable"
    );
    private TypeElementConverterImpl typeElementConverter;
    private PackageProvider packageProvider;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        typeElementConverter = new TypeElementConverterImpl(processingEnv);
        packageProvider = new PackageProvider();
    }

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
        String packageName = packageProvider.getPackage(qualifiedName);
        String storageImplName = "Slinger" + entity.getSimpleName();

        List<StorageMethod> methods = getStorageMethods(entity);

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName + "." + storageImplName);
        BufferedWriter bw = new BufferedWriter(jfo.openWriter());
        try {
            DatabaseStorageBuilder.builder(bw)
                    .setPackage(packageName)
                    .setClassName(storageImplName)
                    .setStorageInterfaceName(entity.getSimpleName().toString())
                    .addMethods(methods)
                    .build();
        } finally {
            bw.close();
        }
    }

    private List<StorageMethod> getStorageMethods(TypeElement element) throws InvalidElementException {
        if(element == null) throw new IllegalArgumentException("element is null");

        List<StorageMethod> methods = new ArrayList<StorageMethod>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                methods.add(createStorageMethod((ExecutableElement) enclosedElement));
            }
        }
        return methods;
    }

    private StorageMethod createStorageMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkFirstParameterMustBeSQLiteDatabase(methodElement);

        if (isAnnotationPresent(methodElement, Insert.class)) {
            return createInsertMethod(methodElement);
        } else if (isAnnotationPresent(methodElement, Replace.class)) {
            return createReplaceMethod(methodElement);
        } else if (isAnnotationPresent(methodElement, Update.class)) {
            return createUpdateMethod(methodElement);
        } else if (isAnnotationPresent(methodElement, Delete.class)) {
            return createDeleteMethod(methodElement);
        } else if (isAnnotationPresent(methodElement, Select.class)) {
            return createSelectMethod(methodElement);
        } else if(isAnnotationPresent(methodElement, CreateTable.class)) {
            return createCreateTableMethod(methodElement);
        } else {
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must be annotated with either @CreateTable, @Insert, @Replace, @Update, @Delete or @Select", methodElement);
        }
    }

    private MapperDescription getMapperDescription(TypeElement databaseEntity) throws InvalidElementException {
        DatabaseEntity entityAnnotation = databaseEntity.getAnnotation(DatabaseEntity.class);
        TypeElement serializerType = getSerializerType(entityAnnotation);
        boolean emptyConstructor = DefaultSerializer.class.getCanonicalName().equals(serializerType.getQualifiedName().toString());

        return new MapperDescription(databaseEntity.getQualifiedName().toString(),
                databaseEntity.getSimpleName().toString(),
                emptyConstructor);
    }

    private StorageMethod createCreateTableMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(CreateTable.class, methodElement);
        checkHasVoidReturnType(methodElement);

        CreateTable createTable = methodElement.getAnnotation(CreateTable.class);
        TypeElement databaseEntity = getCreateTableDatabaseEntity(createTable);
        MapperDescription mapperDescription = getMapperDescription(databaseEntity);

        return new CreateTableMethod(methodElement.getSimpleName().toString(), mapperDescription);
    }

    private StorageMethod createSelectMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Select.class, methodElement);

        Select selectAnnotation = methodElement.getAnnotation(Select.class);
        String where = selectAnnotation.where().equals("") ? null : selectAnnotation.where();
        String orderBy = selectAnnotation.orderBy().equals("") ? null : selectAnnotation.orderBy();

        int sqlArguments = getSqliteArgumentCount(where);

        TypeMirror returnType = methodElement.getReturnType();
        if(returnType.getKind() != TypeKind.DECLARED)
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", methodElement);


        List<? extends VariableElement> parameters = methodElement.getParameters();
        int methodSqlParams = parameters.size() - 1;
        if(methodSqlParams < 0) methodSqlParams = 0;

        if(sqlArguments != methodSqlParams) {
            throw new InvalidElementException(String.format("the sql where argument has %d arguments, the method contains %d", sqlArguments, methodSqlParams), methodElement);
        }

        Collection<String> parameterGetters = getWhereArgs(parameters);
        String parameterText = getParameterText(parameters);

        TypeElement returnTypeElement = (TypeElement) ((DeclaredType) returnType).asElement();
        if(returnTypeElement.getAnnotation(DatabaseEntity.class) != null) {
            MapperDescription mapperDescription = getMapperDescription(returnTypeElement);

            return new SelectSingleMethod(methodElement.getSimpleName().toString(),
                    returnTypeElement.getSimpleName().toString(),
                    parameterText,
                    where,
                    parameterGetters,
                    mapperDescription);
        }
        else if(SUPPORTED_RETURN_TYPES_FOR_SELECT.contains(returnTypeElement.getQualifiedName().toString())) {
            TypeMirror typeMirror = ((DeclaredType) returnType).getTypeArguments().get(0);
            TypeElement databaseEntityElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
            String returnTypeName = databaseEntityElement.getSimpleName().toString();

            MapperDescription mapperDescription = getMapperDescription(databaseEntityElement);

            return new SelectMultipleMethod(methodElement.getSimpleName().toString(),
                    returnTypeName,
                    returnTypeElement.getSimpleName() + "<" + returnTypeName + ">",
                    parameterText,
                    where,
                    parameterGetters,
                    orderBy,
                    mapperDescription);
        }
        else {
            throw new InvalidElementException("Method " + methodElement.getSimpleName() + " must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", methodElement);
        }
    }

    private List<String> getWhereArgs(List<? extends VariableElement> parameters) {
        List<String> whereArgs = new ArrayList<String>();
        for(int i=1;i<parameters.size();i++) {
            VariableElement parameter = parameters.get(i);
            if(ElementUtils.isString(parameter)) {
                whereArgs.add(parameter.getSimpleName().toString());
            }
            else if(ElementUtils.getTypeKind(parameter) == TypeKind.BOOLEAN) {
                whereArgs.add(parameter.getSimpleName() + " ? \"1\" : \"0\"");
            }
            else {
                whereArgs.add("String.valueOf(" + parameter.getSimpleName() + ")");
            }
        }
        return whereArgs;
    }

    private int getSqliteArgumentCount(String where) {
        if(where == null) return 0;

        int count = 0;
        for(int i=0;i<where.length();i++) {
            if(where.charAt(i) == '?') count++;
        }
        return count;
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

        String[] params = new String[parameters.size()];
        for(int i=0;i<parameters.size();i++) {
            VariableElement variableElement = parameters.get(i);
            String typeName = getTypeName(variableElement.asType().getKind(), variableElement);
            params[i] = typeName + " " + variableElement.getSimpleName();
        }
        return String.join(", ", params);
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
        DatabaseEntityModel databaseEntityModel = new DatabaseEntityModel(databaseEntityElement, typeElementConverter);

        String where = databaseEntityModel.getPrimaryKeyDbName() + "=?";
        Element primaryKeyField = databaseEntityModel.getPrimaryKeyField();
        FieldMethod directGetter = databaseEntityModel.findDirectGetter(primaryKeyField);
        List<String> args = Collections.singletonList(directGetter.getMethod());

        MapperDescription mapperDescription = getMapperDescription(databaseEntityElement);

        return new DeleteMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                where, args, mapperDescription);
    }

    private StorageMethod createUpdateMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Update.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);
        DatabaseEntityModel databaseEntityModel = new DatabaseEntityModel(databaseEntityElement, typeElementConverter);

        String where = databaseEntityModel.getPrimaryKeyDbName() + "=?";
        Element primaryKeyField = databaseEntityModel.getPrimaryKeyField();
        FieldMethod directGetter = databaseEntityModel.findDirectGetter(primaryKeyField);
        List<String> args = Collections.singletonList(directGetter.getMethod());

        MapperDescription mapperDescription = getMapperDescription(databaseEntityElement);

        return new UpdateMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                where, args, mapperDescription);
    }

    private StorageMethod createReplaceMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Replace.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);
        checkHasTwoParameters(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);
        MapperDescription mapperDescription = getMapperDescription(databaseEntityElement);

        return new ReplaceMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                mapperDescription
        );
    }

    private StorageMethod createInsertMethod(ExecutableElement methodElement) throws InvalidElementException {
        if(methodElement == null) throw new IllegalArgumentException("methodElement is null");

        checkUniqueAnnotations(Insert.class, methodElement);
        checkSecondParameterMustBeDatabaseEntity(methodElement);
        checkHasTwoParameters(methodElement);

        TypeElement databaseEntityElement = getDatabaseEntityElementFromSecondParameter(methodElement);
        MapperDescription mapperDescription = getMapperDescription(databaseEntityElement);

        return new InsertMethod(methodElement.getSimpleName().toString(),
                databaseEntityElement.getSimpleName().toString(),
                databaseEntityElement.getQualifiedName().toString(),
                mapperDescription
        );
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

    private TypeElement getSerializerType(DatabaseEntity databaseEntity) {
        if(databaseEntity == null) throw new IllegalArgumentException("createTable is null");

        try {
            databaseEntity.serializer();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return typeElementConverter.asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement getCreateTableDatabaseEntity(CreateTable createTable) {
        if(createTable == null) throw new IllegalArgumentException("createTable is null");

        try {
            createTable.value();
            throw new IllegalStateException("should never reach this line (this is a hack)");
        } catch (MirroredTypeException mte) {
            return typeElementConverter.asTypeElement(mte.getTypeMirror());
        }
    }
}
