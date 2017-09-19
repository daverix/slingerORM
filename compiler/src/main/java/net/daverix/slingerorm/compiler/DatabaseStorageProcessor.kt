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
package net.daverix.slingerorm.compiler

import com.squareup.javapoet.*
import net.daverix.slingerorm.DataContainer
import net.daverix.slingerorm.DataPointer
import net.daverix.slingerorm.Mapper
import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.*
import java.io.IOException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

/**
 * This Processor creates Implementations of interfaces annotated with [DatabaseStorage]
 */
open class DatabaseStorageProcessor : AbstractProcessor() {
    override fun process(typeElements: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        for (entity in roundEnvironment.getElementsAnnotatedWith(DatabaseStorage::class.java)) {
            try {
                createJavaFile(entity as TypeElement).writeTo(processingEnv.filer)
            } catch (e: IOException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.localizedMessage)
            } catch (e: InvalidElementException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.message, e.element)
            } catch (e: Exception) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Internal error: " + e.getStackTraceString(""))
            }
        }
        return true // no further processing of this annotation type
    }

    @Throws(IOException::class, InvalidElementException::class)
    private fun createJavaFile(element: TypeElement): JavaFile {
        return createDatabaseStorage(element) {
            methods = getStorageMethods(element)
            mappers = getMappers(element)
        }
    }

    private fun createDatabaseStorage(element: TypeElement, init: DatabaseStorageBuilder.() -> Unit): JavaFile {
        val packageName = element.getPackageName()
        val interfaceName = TypeName.get(element.asType())
        val storageName = "Slinger${element.simpleName}"

        val builder = DatabaseStorageBuilder(packageName, interfaceName, storageName)
        builder.init()
        return builder.build()
    }

    @Throws(InvalidElementException::class)
    private fun getStorageMethods(element: TypeElement): List<MethodSpec> {
        return element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { createStorageMethod(it as ExecutableElement) }
    }

    @Throws(InvalidElementException::class)
    private fun getMappers(element: TypeElement): List<MapperInfo> {
        return element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { getDatabaseEntity(it as ExecutableElement) }
                .distinctBy { it.qualifiedName }
                .map { createMapperInfo(it) }
    }

    @Throws(InvalidElementException::class)
    private fun getDatabaseEntity(element: ExecutableElement): TypeElement {
        return when {
            element.isAnnotationPresent(Insert::class.java) or
                    element.isAnnotationPresent(Replace::class.java) or
                    element.isAnnotationPresent(Update::class.java) -> {
                element.getDatabaseEntityElementFromFirstParameter()
            }
            element.isAnnotationPresent(Delete::class.java) -> {
                if (element.isAnnotationPresent(Where::class.java)) {
                    val delete = element.getAnnotation(Delete::class.java)
                    delete.getTypeElement()
                } else {
                    element.getDatabaseEntityElementFromFirstParameter()
                }
            }
            element.isAnnotationPresent(Select::class.java) -> {
                val returnTypeElement = (element.returnType as DeclaredType).asElement() as TypeElement
                if(returnTypeElement.isAnnotatedWith(DatabaseEntity::class.java)) {
                    returnTypeElement
                } else {
                    // for example get type in a list... List<SomeType>
                    val typeMirror = (element.returnType as DeclaredType).typeArguments[0]
                    (typeMirror as DeclaredType).asElement() as TypeElement
                }
            }
            element.isAnnotationPresent(CreateTable::class.java) -> {
                val createTable = element.getAnnotation(CreateTable::class.java)
                createTable.getTypeElement()
            }
            else -> throw InvalidElementException("Method ${element.simpleName} must be annotated with either @CreateTable, @Insert, @Replace, @Update, @Delete or @Select", element)
        }
    }

    private fun createMapperInfo(databaseEntity: TypeElement): MapperInfo {
        val mapperName = databaseEntity.toMapperVariableName()
        val databaseEntityName = TypeName.get(databaseEntity.asType())
        val typedMapperClassName = getMapperTypeName(databaseEntityName)
        val implementationName = ClassName.get(databaseEntity.getPackageName(),
                "${databaseEntity.simpleName}Mapper")

        return MapperInfo(mapperName,
                typedMapperClassName,
                implementationName,
                databaseEntityName,
                databaseEntity.mapperHasDependencies())
    }

    private fun getMapperTypeName(databaseEntityName: TypeName): ParameterizedTypeName {
        val mapperClassName = ClassName.get(Mapper::class.java)
        return ParameterizedTypeName.get(mapperClassName, databaseEntityName)
    }

    @Throws(InvalidElementException::class)
    private fun createStorageMethod(element: ExecutableElement): MethodSpec {
        return when {
            element.isAnnotationPresent(Insert::class.java) -> createInsertMethod(element)
            element.isAnnotationPresent(Replace::class.java) -> createReplaceMethod(element)
            element.isAnnotationPresent(Update::class.java) -> createUpdateMethod(element)
            element.isAnnotationPresent(Delete::class.java) -> createDeleteMethod(element)
            element.isAnnotationPresent(Select::class.java) -> createSelectMethod(element)
            element.isAnnotationPresent(CreateTable::class.java) -> createCreateTableMethod(element)
            else -> throw InvalidElementException("Method ${element.simpleName} must be annotated with either @CreateTable, @Insert, @Replace, @Update, @Delete or @Select", element)
        }
    }

    @Throws(InvalidElementException::class)
    private fun createCreateTableMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(CreateTable::class.java)
        element.checkHasVoidReturnType()

        val createTable = element.getAnnotation(CreateTable::class.java)
        val databaseEntity = createTable.getTypeElement()
        val mapperVariableName = databaseEntity.toMapperVariableName()

        return MethodSpec.overriding(element)
                .addStatement("db.execSQL($mapperVariableName.createTable())")
                .build()
    }

    private fun TypeElement.toMapperVariableName() =
            "${simpleName.toString().firstCharLowerCase()}Mapper"

    @Throws(InvalidElementException::class)
    private fun createSelectMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(Select::class.java)

        val whereAnnotation = element.getAnnotation(Where::class.java)
        val orderByAnnotation = element.getAnnotation(OrderBy::class.java)
        val limitAnnotation = element.getAnnotation(Limit::class.java)
        val where = whereAnnotation?.value
        val orderBy = orderByAnnotation?.value
        val limit = limitAnnotation?.value

        val sqlArguments = where?.countSqliteArgs()

        val returnType = element.returnType
        if (returnType.kind != TypeKind.DECLARED)
            throw InvalidElementException("Method ${element.simpleName} must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", element)

        val methodSqlParams = element.parameters.size

        if (where != null && sqlArguments != methodSqlParams) {
            throw InvalidElementException(String.format(Locale.ENGLISH,
                    "the sql where $where has %d arguments, the method contains %d parameters. The number of arguments and parameters should match.",
                    sqlArguments, methodSqlParams), element)
        }

        val parameterArguments = element.parameters.getWhereArgs()
        val returnTypeElement = (returnType as DeclaredType).asElement() as TypeElement
        val methodName = element.simpleName.toString()

        when {
            returnTypeElement.isAnnotatedWith(DatabaseEntity::class.java) -> {
                return createSingleSelectMethod(element,
                        returnTypeElement.toMapperVariableName(),
                        where ?: createWhereWithPrimaryKey(returnType),
                        parameterArguments)
            }
            SUPPORTED_RETURN_TYPES_FOR_SELECT.contains(returnTypeElement.qualifiedName.toString()) -> {
                val typeMirror = returnType.typeArguments[0]
                val databaseEntityElement = (typeMirror as DeclaredType).asElement() as TypeElement
                val mapperVariable = databaseEntityElement.toMapperVariableName()

                return createMultiSelectMethod(element,
                        mapperVariable,
                        where,
                        parameterArguments,
                        orderBy,
                        limit)
            }
            else -> throw InvalidElementException("Method $methodName must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", element)
        }
    }

    private fun createSingleSelectMethod(element: ExecutableElement,
                                         mapperVariable: String,
                                         where: String,
                                         parameterArguments: String): MethodSpec {
        val code = CodeBlock.builder()
                .add("pointer = db.query(false,\n")
                .add("        $mapperVariable.getTableName(),\n")
                .add("        $mapperVariable.getFieldNames(),\n")
                .add("        \$S,\n", where)
                .add("        $parameterArguments,\n")
                .add("        null,\n")
                .add("        null,\n")
                .add("        null,\n")
                .add("        \$S);\n\n", 1)
                .build()

        return MethodSpec.overriding(element)
                .addStatement("\$T pointer = null", DataPointer::class.java)
                .beginControlFlow("try")
                .addCode(code)
                .beginControlFlow("if (!pointer.moveToFirst())")
                .addStatement("return null")
                .endControlFlow()
                .addStatement("return $mapperVariable.mapItem(pointer)")
                .nextControlFlow("finally")
                .addStatement("if(pointer != null) pointer.close()")
                .endControlFlow()
                .build()
    }

    private fun createMultiSelectMethod(element: ExecutableElement,
                                        mapperVariable: String,
                                        where: String?,
                                        parameterArguments: String,
                                        orderBy: String?,
                                        limit: String?): MethodSpec {
        val code = CodeBlock.builder()
                .add("pointer = db.query(false,\n")
                .add("        $mapperVariable.getTableName(),\n")
                .add("        $mapperVariable.getFieldNames(),\n")
                .add("        ${if (where == null) "null" else "\"$where\""},\n")
                .add("        $parameterArguments,\n")
                .add("        null,\n")
                .add("        null,\n")
                .add("        ${if (orderBy == null) "null" else "\"$orderBy\""},\n")
                .add("        ${if (limit == null) "null" else "\"$limit\""});\n\n")
                .build()

        return MethodSpec.overriding(element)
                .addStatement("\$T pointer = null", DataPointer::class.java)
                .beginControlFlow("try")
                .addCode(code)
                .addStatement("return $mapperVariable.mapList(pointer)")
                .nextControlFlow("finally")
                .addStatement("if(pointer != null) pointer.close()")
                .endControlFlow()
                .build()
    }

    @Throws(InvalidElementException::class)
    private fun createDeleteMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(Delete::class.java)

        val returnTypeKind = element.returnType.kind
        if (returnTypeKind != TypeKind.INT && returnTypeKind != TypeKind.VOID)
            throw InvalidElementException("Only int and void are supported as return types for Delete annotated methods", element)

        val whereAnnotation = element.getAnnotation(Where::class.java)
        if (whereAnnotation == null) {
            element.checkFirstParameterMustBeDatabaseEntity()

            val databaseEntityElement = element.getDatabaseEntityElementFromFirstParameter()
            return createDeleteSingleMethod(element, databaseEntityElement.toMapperVariableName())
        }

        val delete = element.getAnnotation(Delete::class.java)
        val databaseEntityElement = delete.getTypeElement()
        if ("java.lang.Object" == databaseEntityElement.qualifiedName.toString())
            throw InvalidElementException("Where together with Delete requires the type to delete to be set in Delete annotation", element)

        val where = whereAnnotation.value
        val sqlArgumentsCount = where.countSqliteArgs()

        if (sqlArgumentsCount != element.parameters.size) {
            throw InvalidElementException("the sql where argument has $sqlArgumentsCount arguments, the method contains ${element.parameters.size}", element)
        }

        val whereArgs = element.parameters.getWhereArgs()

        return createDeleteWhereMethod(element,
                databaseEntityElement.toMapperVariableName(),
                where,
                whereArgs)
    }

    private fun createDeleteSingleMethod(element: ExecutableElement,
                                         mapperVariable: String): MethodSpec {
        val parameterName = element.parameters[0].simpleName.toString()
        val codeBlock = CodeBlock.builder()
                .add("${if (element.returnType.kind == TypeKind.VOID) {
                    ""
                } else {
                    "return "
                }}db.delete($mapperVariable.getTableName(),\n")
                .add("        $mapperVariable.getItemQuery(),\n")
                .add("        $mapperVariable.getItemQueryArguments($parameterName));\n")
                .build()

        return MethodSpec.overriding(element)
                .addStatement("if ($parameterName == null) throw new \$T(\"$parameterName is null\")",
                        IllegalArgumentException::class.java)
                .addCode("\n")
                .addCode(codeBlock)
                .build()
    }

    private fun createDeleteWhereMethod(element: ExecutableElement,
                                        mapperVariable: String,
                                        where: String?,
                                        whereArgs: String): MethodSpec {
        val codeBlock = CodeBlock.builder()
                .add("${if (element.returnType.kind == TypeKind.VOID) {
                    ""
                } else {
                    "return "
                }}db.delete($mapperVariable.getTableName(),\n")
                .add("        ${if (where == null) "null" else "\"$where\""},\n")
                .add("        $whereArgs);\n")
                .build()

        return MethodSpec.overriding(element)
                .addCode(codeBlock)
                .build()
    }

    @Throws(InvalidElementException::class)
    private fun createUpdateMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(Update::class.java)
        element.checkFirstParameterMustBeDatabaseEntity()

        val databaseEntityElement = element.getDatabaseEntityElementFromFirstParameter()

        val parameterName = element.parameters[0].simpleName
        val mapperVariable = databaseEntityElement.toMapperVariableName()

        return MethodSpec.overriding(element)
                .addStatement("if ($parameterName == null) throw new \$T(\"$parameterName is null\")",
                        IllegalArgumentException::class.java)
                .addCode("\n")
                .addStatement("\$T container = db.edit($mapperVariable.getTableName())",
                        DataContainer::class.java)
                .addStatement("$mapperVariable.mapValues($parameterName, container)")
                .addStatement("${if(element.returnType.kind == TypeKind.VOID) {
                    ""
                } else {
                    "return "
                }}container.update($mapperVariable.getItemQuery(), $mapperVariable.getItemQueryArguments($parameterName))")
                .build()
    }

    @Throws(InvalidElementException::class)
    private fun createReplaceMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(Replace::class.java)
        element.checkFirstParameterMustBeDatabaseEntity()
        element.checkHasOneParameter()

        val databaseEntityElement = element.getDatabaseEntityElementFromFirstParameter()
        val mapperVariable = databaseEntityElement.toMapperVariableName()
        val parameterName = element.parameters[0].simpleName

        return MethodSpec.overriding(element)
                .addStatement("if ($parameterName == null) throw new \$T(\"$parameterName is null\")",
                        IllegalArgumentException::class.java)
                .addCode("\n")
                .addStatement("\$T container = db.edit($mapperVariable.getTableName())",
                        DataContainer::class.java)
                .addStatement("$mapperVariable.mapValues($parameterName, container)")
                .addStatement("${if(element.returnType.kind == TypeKind.VOID) {
                    ""
                } else {
                    "return "
                }}container.replace()")
                .build()
    }

    @Throws(InvalidElementException::class)
    private fun createInsertMethod(element: ExecutableElement): MethodSpec {
        element.checkUniqueAnnotations(Insert::class.java)
        element.checkFirstParameterMustBeDatabaseEntity()
        element.checkHasOneParameter()

        val databaseEntityElement = element.getDatabaseEntityElementFromFirstParameter()
        val mapperVariable = databaseEntityElement.toMapperVariableName()
        val parameterName = element.parameters[0].simpleName

        return MethodSpec.overriding(element)
                .addStatement("if ($parameterName == null) throw new \$T(\"$parameterName is null\")",
                        IllegalArgumentException::class.java)
                .addCode("\n")
                .addStatement("\$T container = db.edit($mapperVariable.getTableName())",
                        DataContainer::class.java)
                .addStatement("$mapperVariable.mapValues($parameterName, container)")
                .addStatement("${if(element.returnType.kind == TypeKind.VOID) {
                    ""
                } else {
                    "return "
                }}container.insert()")
                .build()
    }

    private fun createWhereWithPrimaryKey(returnType: TypeMirror): String {
        val typeElement = processingEnv.typeUtils.asElement(returnType) as TypeElement

        return DatabaseEntityModel(typeElement).itemSql
    }

    private fun List<VariableElement>.getWhereArgs(): String {
        val args = map {
            when {
                it.isString() -> it.simpleName.toString()
                it.getTypeKind() === TypeKind.BOOLEAN -> "${it.simpleName} ? \"1\" : \"0\""
                else -> "String.valueOf(${it.simpleName})"
            }
        }.joinToString(", ")

        if(args.isEmpty()) {
            return "null"
        }

        return "new String[]{$args}"
    }

    @Throws(InvalidElementException::class)
    private fun TypeElement.mapperHasDependencies(): Boolean {
        val typeElement = processingEnv.elementUtils.getTypeElement(qualifiedName.toString() + "Mapper.Builder")
        return typeElement != null && typeElement.getMethods()
                .any { method -> "create" == method.simpleName.toString() }
    }

    private fun ExecutableElement.getDatabaseEntityElementFromFirstParameter(): TypeElement {
        val secondParameter = parameters[0]
        return (secondParameter.asType() as DeclaredType).asElement() as TypeElement
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.checkHasVoidReturnType() {
        if (returnType.kind != TypeKind.VOID)
            throw InvalidElementException("Only void is supported as return type for this method", this)
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.checkHasOneParameter() {
        if (parameters.size != 1)
            throw InvalidElementException("method must have exactly one parameters", this)
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.checkFirstParameterMustBeDatabaseEntity() {
        if (parameters.size < 1)
            throw InvalidElementException("method must have at least one parameter where the first is a type annotated with @DatabaseEntity", this)

        val firstParameter = parameters[0]
        if (firstParameter.asType().kind != TypeKind.DECLARED)
            throw InvalidElementException("first parameter must be a declared type annotated with @DatabaseEntity", firstParameter)

        val parameterTypeElement = (firstParameter.asType() as DeclaredType).asElement() as TypeElement
        if (parameterTypeElement.getAnnotation(DatabaseEntity::class.java) == null)
            throw InvalidElementException("first parameter must be annotated with @DatabaseEntity", firstParameter)
    }

    @Throws(InvalidElementException::class)
    private fun <T : Annotation> ExecutableElement.checkUniqueAnnotations(annotationClass: Class<T>) {
        checkUniqueAnnotation(annotationClass, Inject::class.java)
        checkUniqueAnnotation(annotationClass, Replace::class.java)
        checkUniqueAnnotation(annotationClass, Update::class.java)
        checkUniqueAnnotation(annotationClass, Delete::class.java)
        checkUniqueAnnotation(annotationClass, Select::class.java)
        checkUniqueAnnotation(annotationClass, CreateTable::class.java)
    }

    @Throws(InvalidElementException::class)
    private fun <T : Annotation, TOther : Annotation> ExecutableElement.checkUniqueAnnotation(annotationClass: Class<T>,
                                                                                              otherAnnotationClass: Class<TOther>) {
        if (annotationClass != otherAnnotationClass && isAnnotationPresent(otherAnnotationClass)) {
            throw InvalidElementException("Method can't be annotated with both @${annotationClass.simpleName} and @${otherAnnotationClass.simpleName}", this)
        }
    }

    private fun <T : Annotation?> ExecutableElement.isAnnotationPresent(annotationClass: Class<T>): Boolean {
        return getAnnotation<T>(annotationClass) != null
    }

    private fun Delete.getTypeElement(): TypeElement {
        try {
            value
            throw IllegalStateException("should never reach this line (this is a hack)")
        } catch (mte: MirroredTypeException) {
            return processingEnv.typeUtils.asElement(mte.typeMirror) as TypeElement
        }

    }

    private fun CreateTable.getTypeElement(): TypeElement {
        try {
            value
            throw IllegalStateException("should never reach this line (this is a hack)")
        } catch (mte: MirroredTypeException) {
            return processingEnv.typeUtils.asElement(mte.typeMirror) as TypeElement
        }

    }

    companion object {
        private val SUPPORTED_RETURN_TYPES_FOR_SELECT = listOf(
                "java.util.List",
                "java.util.Collection",
                "java.lang.Iterable"
        )
    }
}
