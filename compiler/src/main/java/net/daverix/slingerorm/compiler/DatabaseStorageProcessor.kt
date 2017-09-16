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

import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.*
import java.io.BufferedWriter
import java.io.IOException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
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
    private lateinit var packageProvider: PackageProvider

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        packageProvider = PackageProvider()
    }

    override fun process(typeElements: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        for (entity in roundEnvironment.getElementsAnnotatedWith(DatabaseStorage::class.java)) {
            try {
                (entity as TypeElement).createStorage()
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
    private fun TypeElement.createStorage() {
        val qualifiedName = qualifiedName.toString()
        val packageName = packageProvider.getPackage(qualifiedName)
        val storageImplName = "Slinger$simpleName"

        val methods = getStorageMethods()

        val jfo = processingEnv.filer.createSourceFile("$packageName.$storageImplName")
        BufferedWriter(jfo.openWriter()).use { bw ->
            DatabaseStorageBuilder.builder(bw)
                    .setPackage(packageName)
                    .setClassName(storageImplName)
                    .setStorageInterfaceName(simpleName.toString())
                    .addMethods(methods)
                    .build()
        }
    }

    @Throws(InvalidElementException::class)
    private fun TypeElement.getStorageMethods(): List<StorageMethod> {
        return enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { (it as ExecutableElement).createStorageMethod() }
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createStorageMethod(): StorageMethod {
        return when {
            isAnnotationPresent(Insert::class.java) -> createInsertMethod()
            isAnnotationPresent(Replace::class.java) -> createReplaceMethod()
            isAnnotationPresent(Update::class.java) -> createUpdateMethod()
            isAnnotationPresent(Delete::class.java) -> createDeleteMethod()
            isAnnotationPresent(Select::class.java) -> createSelectMethod()
            isAnnotationPresent(CreateTable::class.java) -> createCreateTableMethod()
            else -> throw InvalidElementException("Method $simpleName must be annotated with either @CreateTable, @Insert, @Replace, @Update, @Delete or @Select", this)
        }
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createCreateTableMethod(): StorageMethod {
        checkUniqueAnnotations(CreateTable::class.java)
        checkHasVoidReturnType()

        val createTable = getAnnotation(CreateTable::class.java)
        val databaseEntity = createTable.getTypeElement()

        return CreateTableMethod(simpleName.toString(),
                "${databaseEntity.qualifiedName}Mapper",
                "${databaseEntity.simpleName.toString().firstCharLowerCase()}Mapper",
                databaseEntity.simpleName.toString(),
                databaseEntity.mapperHasDependencies())
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createSelectMethod(): StorageMethod {
        checkUniqueAnnotations(Select::class.java)

        val whereAnnotation = getAnnotation(Where::class.java)
        val orderByAnnotation = getAnnotation(OrderBy::class.java)
        val limitAnnotation = getAnnotation(Limit::class.java)
        val where = whereAnnotation?.value
        val orderBy = orderByAnnotation?.value
        val limit = limitAnnotation?.value

        val sqlArguments = where?.countSqliteArgs()

        val returnType = returnType
        if (returnType.kind != TypeKind.DECLARED)
            throw InvalidElementException("Method $simpleName must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", this)

        val methodSqlParams = parameters.size

        if (where != null && sqlArguments != methodSqlParams) {
            throw InvalidElementException(String.format(Locale.ENGLISH,
                    "the sql where $where has %d arguments, the method contains %d parameters. The number of arguments and parameters should match.",
                    sqlArguments, methodSqlParams), this)
        }

        val parameterGetters = parameters.getWhereArgs()
        val parameterText = parameters.asParameterText()

        val returnTypeElement = (returnType as DeclaredType).asElement() as TypeElement
        when {
            returnTypeElement.getAnnotation(DatabaseEntity::class.java) != null -> {
                return SelectSingleMethod(simpleName.toString(),
                        parameterText,
                        where ?: createWhereWithPrimaryKey(returnType),
                        parameterGetters,
                        returnTypeElement.simpleName.toString(),
                        "${returnTypeElement.qualifiedName}Mapper",
                        "${returnTypeElement.simpleName.toString().firstCharLowerCase()}Mapper",
                        returnTypeElement.mapperHasDependencies())
            }
            SUPPORTED_RETURN_TYPES_FOR_SELECT.contains(returnTypeElement.qualifiedName.toString()) -> {
                val typeMirror = returnType.typeArguments[0]
                val databaseEntityElement = (typeMirror as DeclaredType).asElement() as TypeElement
                val returnTypeName = databaseEntityElement.simpleName.toString()

                return SelectMultipleMethod(simpleName.toString(),
                        returnTypeElement.simpleName.toString() + "<" + returnTypeName + ">",
                        parameterText,
                        where,
                        parameterGetters,
                        orderBy,
                        limit,
                        databaseEntityElement.simpleName.toString(),
                        "${databaseEntityElement.qualifiedName}Mapper",
                        "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                        databaseEntityElement.mapperHasDependencies())
            }
            else -> throw InvalidElementException("Method $simpleName must return a type annotated with @DatabaseEntity or a list of a type annotated with @DatabaseEntity", this)
        }
    }

    private fun createWhereWithPrimaryKey(returnType: TypeMirror): String {
        val typeElement = processingEnv.typeUtils.asElement(returnType) as TypeElement

        return DatabaseEntityModel(typeElement).itemSql
    }

    private fun List<VariableElement>.getWhereArgs(): List<String> {
        val whereArgs = ArrayList<String>()
        forEach {
            when {
                it.isString() -> whereArgs.add(it.simpleName.toString())
                it.getTypeKind() === TypeKind.BOOLEAN -> whereArgs.add("${it.simpleName} ? \"1\" : \"0\"")
                else -> whereArgs.add("String.valueOf(${it.simpleName})")
            }
        }
        return whereArgs
    }

    @Throws(InvalidElementException::class)
    private fun List<VariableElement>.asParameterText(): String {
        return map {
            return "${it.getTypeName()} ${it.simpleName}"
        }.joinToString(",")
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createDeleteMethod(): StorageMethod {
        checkUniqueAnnotations(Delete::class.java)

        val returnTypeKind = returnType.kind
        if (returnTypeKind != TypeKind.INT && returnTypeKind != TypeKind.VOID)
            throw InvalidElementException("Only int and void are supported as return types for Delete annotated methods", this)

        val returnDeleted = returnTypeKind == TypeKind.INT

        val whereAnnotation = getAnnotation(Where::class.java)
        if (whereAnnotation == null) {
            checkFirstParameterMustBeDatabaseEntity()

            val databaseEntityElement = getDatabaseEntityElementFromFirstParameter()
            return DeleteMethod(simpleName.toString(),
                    returnDeleted,
                    databaseEntityElement.qualifiedName.toString(),
                    databaseEntityElement.simpleName.toString(),
                    "${databaseEntityElement.qualifiedName}Mapper",
                    "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                    databaseEntityElement.mapperHasDependencies())
        }

        val delete = getAnnotation(Delete::class.java)
        val databaseEntityElement = delete.getTypeElement()
        if ("java.lang.Object" == databaseEntityElement.qualifiedName.toString())
            throw InvalidElementException("Where together with Delete requires the type to delete to be set in Delete annotation", this)

        val where = whereAnnotation.value
        val sqlArgumentsCount = where.countSqliteArgs()

        if (sqlArgumentsCount != parameters.size) {
            throw InvalidElementException("the sql where argument has $sqlArgumentsCount arguments, the method contains ${parameters.size}", this)
        }

        val parameterGetters = parameters.getWhereArgs()
        val parameterText = parameters.asParameterText()

        return DeleteWhereMethod(simpleName.toString(),
                returnDeleted,
                parameterText,
                where,
                parameterGetters,
                databaseEntityElement.simpleName.toString(),
                "${databaseEntityElement.qualifiedName}Mapper",
                "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                databaseEntityElement.mapperHasDependencies())
    }

    @Throws(InvalidElementException::class)
    private fun TypeElement.mapperHasDependencies(): Boolean {
        val typeElement = processingEnv.elementUtils.getTypeElement(qualifiedName.toString() + "Mapper.Builder")
        return typeElement != null && typeElement.getMethods()
                .any { method -> "create" == method.simpleName.toString() }
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createUpdateMethod(): StorageMethod {
        checkUniqueAnnotations(Update::class.java)
        checkFirstParameterMustBeDatabaseEntity()

        val databaseEntityElement = getDatabaseEntityElementFromFirstParameter()

        return UpdateMethod(simpleName.toString(),
                databaseEntityElement.qualifiedName.toString(),
                databaseEntityElement.simpleName.toString(),
                "${databaseEntityElement.qualifiedName}Mapper",
                "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                databaseEntityElement.mapperHasDependencies())
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createReplaceMethod(): StorageMethod {
        checkUniqueAnnotations(Replace::class.java)
        checkFirstParameterMustBeDatabaseEntity()
        checkHasOneParameter()

        val databaseEntityElement = getDatabaseEntityElementFromFirstParameter()

        return ReplaceMethod(simpleName.toString(),
                databaseEntityElement.qualifiedName.toString(),
                databaseEntityElement.simpleName.toString(),
                "${databaseEntityElement.qualifiedName}Mapper",
                "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                databaseEntityElement.mapperHasDependencies()
        )
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.createInsertMethod(): StorageMethod {
        checkUniqueAnnotations(Insert::class.java)
        checkFirstParameterMustBeDatabaseEntity()
        checkHasOneParameter()

        val databaseEntityElement = getDatabaseEntityElementFromFirstParameter()

        return InsertMethod(simpleName.toString(),
                databaseEntityElement.qualifiedName.toString(),
                databaseEntityElement.simpleName.toString(),
                "${databaseEntityElement.qualifiedName}Mapper",
                "${databaseEntityElement.simpleName.toString().firstCharLowerCase()}Mapper",
                databaseEntityElement.mapperHasDependencies()
        )
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
