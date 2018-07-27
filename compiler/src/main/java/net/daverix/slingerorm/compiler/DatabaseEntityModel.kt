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

import net.daverix.slingerorm.entity.*
import net.daverix.slingerorm.serializer.SerializeType
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

class DatabaseEntityModel(private val databaseTypeElement: TypeElement) {
    val tableName: String
        @Throws(InvalidElementException::class)
        get() {
            val annotation = databaseTypeElement.getAnnotation(DatabaseEntity::class.java)
                    ?: throw InvalidElementException("element $databaseTypeElement not annotated with @DatabaseEntity", databaseTypeElement)

            val tableName = annotation.name
            return if (tableName == "") databaseTypeElement.simpleName.toString() else tableName

        }

    val fieldNames: Array<String>
        @Throws(InvalidElementException::class)
        get() {
            return fieldsUsedInDatabase.map { getDatabaseFieldName(it) }.toTypedArray()
        }

    val fieldsUsedInDatabase: List<Element>
        @Throws(InvalidElementException::class)
        get() = databaseTypeElement.elements.filter { it.isDatabaseField() }

    @Throws(InvalidElementException::class)
    private fun List<Element>.getDatabaseFieldNames(): List<String> {
        return map { getDatabaseFieldName(it) }
    }

    @Throws(InvalidElementException::class)
    fun getDatabaseFieldName(element: Element): String {
        val fieldNameAnnotation = element.getAnnotation(FieldName::class.java)
                ?: return element.simpleName.toString()

        val fieldName = fieldNameAnnotation.value
        if (fieldName == "")
            throw InvalidElementException("fieldName must not be null or empty!", element)

        return fieldName
    }

    val primaryKeyFieldNames: Set<String> by lazy {
        val entityAnnotation = databaseTypeElement.getAnnotation(DatabaseEntity::class.java)
        val annotationKeys = entityAnnotation.primaryKeyFields
                .filter { x -> !x.isEmpty() }
                .toSet()

        if (annotationKeys.isNotEmpty()) {
            annotationKeys
        } else {
            primaryKeyFields
                    .map { it.simpleName.toString() }
                    .filter { x -> !x.isEmpty() }
                    .toSet()
        }
    }

    private val primaryKeyDbNames: List<String> by lazy {
        primaryKeyFields.getDatabaseFieldNames()
    }

    private val primaryKeyFields: List<Element>
        @Throws(InvalidElementException::class)
        get() {
            var fields: List<Element> = fieldsUsedInDatabase

            val primaryKeysByAnnotation = fields.filterByAnnotation(PrimaryKey::class.java)
            if (primaryKeysByAnnotation.isEmpty())
                fields = fields.getPrimaryKeyFieldsUsingDatabaseEntity()

            if (fields.isEmpty())
                throw InvalidElementException("There must be a field annotated with PrimaryKey or the keys specified in @DatabaseEntity is empty!", databaseTypeElement)

            return fields
        }

    @Throws(InvalidElementException::class)
    private fun List<Element>.getPrimaryKeyFieldsUsingDatabaseEntity(): List<Element> {
        val annotation = databaseTypeElement.getAnnotation(DatabaseEntity::class.java)
        val keys = annotation.primaryKeyFields
        if (keys.isEmpty())
            return emptyList()

        val elements = ArrayList<Element>()
        for (key in keys) {
            val field = getFieldByName(key)
                    ?: throw InvalidElementException("Field specified in DatabaseEntity annotation doesn't exist in entity class!", databaseTypeElement)

            elements.add(field)
        }

        return elements
    }

    private fun Element.isDatabaseField(): Boolean {
        if (kind != ElementKind.FIELD)
            return false

        return Modifier.STATIC !in modifiers &&
                Modifier.TRANSIENT !in modifiers &&
                !isAnnotatedWith<IgnoreField>()
    }

    private val SerializeType.databaseType: String
        get() = when (this) {
            SerializeType.SHORT, SerializeType.INT, SerializeType.LONG -> "INTEGER"
            SerializeType.FLOAT, SerializeType.DOUBLE -> "REAL"
            SerializeType.BYTE_ARRAY -> "BLOB"
            else -> "TEXT"
        }

    @Throws(InvalidElementException::class)
    fun getDatabaseType(element: Element): String {
        val fieldType = element.asType()
        val typeKind = fieldType.kind
        when (typeKind) {
            TypeKind.BOOLEAN -> return SerializeType.INT.databaseType
            TypeKind.SHORT -> return SerializeType.SHORT.databaseType
            TypeKind.LONG -> return SerializeType.LONG.databaseType
            TypeKind.INT -> return SerializeType.INT.databaseType
            TypeKind.FLOAT -> return SerializeType.FLOAT.databaseType
            TypeKind.DOUBLE -> return SerializeType.DOUBLE.databaseType
            TypeKind.DECLARED -> {
                val declaredType = fieldType as DeclaredType
                val typeElement = declaredType.asElement() as TypeElement
                val typeName = typeElement.qualifiedName.toString()

                return if (typeName == TYPE_STRING) {
                    SerializeType.STRING.databaseType
                } else {
                    val annotation = element.getAnnotation(SerializeTo::class.java)
                            ?: throw InvalidElementException("$typeName is not a type that SlingerORM understands, please add @SerializeTo and tell it what to serialize to.", element)

                    annotation.value.databaseType
                }
            }
            else -> throw InvalidElementException(element.simpleName.toString() + " have a type not known by SlingerORM, solve this by creating a custom serializer", element)
        }
    }

    @Throws(InvalidElementException::class)
    private fun <T : Annotation?> List<Element>.filterByAnnotation(annotationClass: Class<T>): List<Element> {
        return filter { item ->
            item.getAnnotation<T>(annotationClass) != null
        }.toList()
    }

    @Throws(InvalidElementException::class)
    private fun List<Element>.getFieldByName(name: String): Element? {
        return firstOrNull { item -> name == item.simpleName.toString() }
    }

    @Throws(InvalidElementException::class)
    private fun Element.findGetter(variableName: String): String {
        val objectType = getObjectTypeForElement()
        return when (objectType) {
            ObjectType.BOOLEAN,
            ObjectType.DOUBLE,
            ObjectType.FLOAT,
            ObjectType.INT,
            ObjectType.LONG,
            ObjectType.SHORT,
            ObjectType.STRING -> findDirectGetter(variableName)
            ObjectType.OTHER -> getSerializerMethod(variableName)
        }
    }

    @Throws(InvalidElementException::class)
    private fun Element.getSerializerMethod(variableName: String): String {
        val serializerFieldName = getSerializerFieldName()
        val getter = findDirectGetter(variableName)
        return "$serializerFieldName.serialize($getter)"
    }

    @Throws(InvalidElementException::class)
    private fun Element.getSerializerFieldName(): String {
        val typeElement = asTypeElement()
        val fieldTypeName = typeElement.simpleName.toString()
        val firstLowerCase = fieldTypeName.substring(0, 1).toLowerCase() + fieldTypeName.substring(1)

        val annotation = getAnnotation(SerializeTo::class.java)
        return "${firstLowerCase}To${annotation.value.getTypeName()}Serializer"
    }

    private fun SerializeType.getTypeName(): String {
        return when (this) {
            SerializeType.SHORT -> "Short"
            SerializeType.INT -> "Int"
            SerializeType.LONG -> "Long"
            SerializeType.FLOAT -> "Float"
            SerializeType.DOUBLE -> "Double"
            SerializeType.STRING -> "String"
            SerializeType.BYTE_ARRAY -> "ByteArray"
        }
    }

    @Throws(InvalidElementException::class)
    private fun Element.findDirectGetter(variableName: String): String {
        val methodsInDatabaseEntityElement = databaseTypeElement.methods
        var method = findMethodByFieldNameAndGetFieldAnnotation(methodsInDatabaseEntityElement, simpleName.toString())
        if (method != null)
            return "$variableName.${method.simpleName}()"

        val isBoolean = asType().kind == TypeKind.BOOLEAN
        method = methodsInDatabaseEntityElement.firstByFieldName(simpleName.toString(),
                if (isBoolean) "is" else "get")

        if (method != null)
            return "$variableName.${method.simpleName}()"

        if (!isAccessible)
            throw InvalidElementException("No get method or a public field for $simpleName in ${databaseTypeElement.simpleName}",
                    databaseTypeElement)

        return "$variableName.$simpleName"
    }

    @Throws(InvalidElementException::class)
    private fun List<ExecutableElement>.firstByFieldName(fieldName: String, prefix: String): ExecutableElement? {
        return firstOrNull { item ->
            val firstLetter = fieldName.substring(0, 1).toUpperCase()
            val methodName = prefix + firstLetter + fieldName.substring(1)

            methodName == item.simpleName.toString()
        }
    }

    @Throws(InvalidElementException::class)
    private fun findMethodByFieldNameAndGetFieldAnnotation(elements: List<ExecutableElement>, fieldName: String): ExecutableElement? {
        for (element in elements) {
            val annotation = element.getAnnotation(GetField::class.java) ?: continue

            val fieldReference = annotation.value
            if (fieldReference == "")
                throw InvalidElementException(element.simpleName.toString() + " has a GetField annotation with empty value!", element)

            if (fieldReference == fieldName)
                return element
        }

        return null
    }

    @Throws(InvalidElementException::class)
    private fun Element.getObjectTypeForElement(): ObjectType {
        when (typeKind) {
            TypeKind.BOOLEAN -> return ObjectType.BOOLEAN
            TypeKind.SHORT -> return ObjectType.SHORT
            TypeKind.INT -> return ObjectType.INT
            TypeKind.LONG -> return ObjectType.LONG
            TypeKind.FLOAT -> return ObjectType.FLOAT
            TypeKind.DOUBLE -> return ObjectType.DOUBLE
            TypeKind.DECLARED -> {
                val declaredType = asType() as DeclaredType
                val typeElement = declaredType.asElement() as TypeElement
                val typeName = typeElement.qualifiedName.toString()
                return if (typeName == TYPE_STRING) {
                    ObjectType.STRING
                } else {
                    ObjectType.OTHER
                }
            }
            TypeKind.ARRAY, TypeKind.BYTE, TypeKind.CHAR -> throw InvalidElementException(typeKind.toString() + " is not known by SlingerORM, solve this by creating a custom serializer method", this)
            else -> throw IllegalStateException(typeKind.toString() + " should never been reached, processor error?")
        }
    }

    @Throws(InvalidElementException::class)
    private fun List<ExecutableElement>.firstByFieldNameAndSetFieldAnnotation(fieldName: String): ExecutableElement? {
        for (element in this) {
            val annotation = element.getAnnotation(SetField::class.java) ?: continue

            val fieldReference = annotation.value
            if (fieldReference == "")
                throw InvalidElementException(element.simpleName.toString() + " has a SetField annotation with empty value!", element)

            if (fieldReference == fieldName)
                return element
        }

        return null
    }

    @Throws(InvalidElementException::class)
    private fun Element.findSetter(variableName: String, columnIndex: Int): String {
        val methodsInDatabaseEntityElement = databaseTypeElement.methods
        var method = methodsInDatabaseEntityElement.firstByFieldNameAndSetFieldAnnotation(simpleName.toString())
        if (method != null) {
            return method.findSetterMethodFromParameter(this, variableName, columnIndex)
        }

        method = methodsInDatabaseEntityElement.firstByFieldName(simpleName.toString(), "set")
        if (method != null) {
            return method.findSetterMethodFromParameter(this, variableName, columnIndex)
        }

        if (!isAccessible)
            throw InvalidElementException("No get method or a public field for " + simpleName + " in " + databaseTypeElement.simpleName, this)

        return "$simpleName = ${findCursorMethod(this, variableName, columnIndex)}"
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.findSetterMethodFromParameter(field: Element, variableName: String, columnIndex: Int): String {
        if (parameters.size != 1)
            throw InvalidElementException("method has ${parameters.size} parameters, only 1 parameter supported!", this)

        return "$simpleName(${parameters[0].findCursorMethod(field, variableName, columnIndex)})"
    }

    @Throws(InvalidElementException::class)
    private fun Element.findCursorMethod(field: Element, variableName: String, columnIndex: Int): String {
        val objectType = getObjectTypeForElement()
        return if (objectType == ObjectType.OTHER) {
            findDeserializerMethod(field, variableName, columnIndex)
        } else {
            getCursorMethod(objectType, variableName, columnIndex)
        }
    }

    @Throws(InvalidElementException::class)
    private fun getCursorMethod(objectType: ObjectType, variableName: String, columnIndex: Int): String {
        return when (objectType) {
            ObjectType.BOOLEAN -> "$variableName.getInt($columnIndex) == 1"
            ObjectType.DOUBLE -> "$variableName.getDouble($columnIndex)"
            ObjectType.FLOAT -> "$variableName.getFloat($columnIndex)"
            ObjectType.INT -> "$variableName.getInt($columnIndex)"
            ObjectType.LONG -> "$variableName.getLong($columnIndex)"
            ObjectType.SHORT -> "$variableName.getShort($columnIndex)"
            ObjectType.STRING -> "$variableName.getString($columnIndex)"
            else -> throw UnsupportedOperationException("this should not be called!")
        }
    }

    @Throws(InvalidElementException::class)
    private fun findDeserializerMethod(field: Element, variableName: String, columnIndex: Int): String {
        val serializerFieldName = field.getSerializerFieldName()
        val annotation = field.getAnnotation(SerializeTo::class.java)
        val cursorMethod = getCursorMethod(annotation.value.asObjectType(), variableName, columnIndex)

        return "$serializerFieldName.deserialize($cursorMethod)"
    }

    private fun SerializeType.asObjectType(): ObjectType {
        return when (this) {
            SerializeType.SHORT -> ObjectType.SHORT
            SerializeType.INT -> ObjectType.INT
            SerializeType.LONG -> ObjectType.LONG
            SerializeType.FLOAT -> ObjectType.FLOAT
            SerializeType.DOUBLE -> ObjectType.DOUBLE
            SerializeType.STRING -> ObjectType.STRING
            else -> throw UnsupportedOperationException(toString() + " not implemented")
        }
    }

    fun getSetters(variableName: String): List<String> =
            fieldsUsedInDatabase.mapIndexed { columnIndex, element -> element.findSetter(variableName, columnIndex) }

    fun getGetters(variableName: String): List<String> =
            fieldsUsedInDatabase.map { createGetter(it, variableName) }

    @Throws(InvalidElementException::class)
    private fun createGetter(field: Element, variableName: String): String {
        return "\"${getDatabaseFieldName(field)}\", ${field.findGetter(variableName)}"
    }

    val itemSql: String
        @Throws(InvalidElementException::class)
        get() = primaryKeyDbNames
                .stream()
                .map { key -> "$key = ?" }
                .reduce { a, b -> "$a AND $b" }
                .orElse("")

    fun getItemSqlArgs(variableName: String): List<String> = primaryKeyFields.map {
        it.findDirectGetter(variableName).encloseStringValueOfIfNotString(it.isString())
    }

    val serializers: List<SerializerType>
        @Throws(InvalidElementException::class)
        get() {
            return fieldsUsedInDatabase.filter {
                it.getAnnotation(SerializeTo::class.java) != null
            }.map {
                val annotation = it.getAnnotation(SerializeTo::class.java)
                val serializedType = annotation.value.getTypeName()
                val deserializedType = it.getFieldTypeName()
                val imports = it.getImports()

                SerializerType(it.getSerializerFieldName(),
                        "Serializer<$deserializedType,$serializedType>",
                        imports)
            }
        }

    private fun Element.getImports(): List<String> {
        val imports = ArrayList<String>()
        imports.add("net.daverix.slingerorm.serializer.Serializer")

        if (typeKind == TypeKind.DECLARED) {
            val declaredType = asType() as DeclaredType
            val typeElement = declaredType.asElement() as TypeElement
            imports.add(typeElement.qualifiedName.toString())
        }
        return imports
    }

    private fun Element.getFieldTypeName(): String {
        return when (typeKind) {
            TypeKind.DECLARED -> {
                val declaredType = asType() as DeclaredType
                declaredType.getSimpleName()
            }
            TypeKind.ARRAY -> {
                val arrayType = asType() as ArrayType
                val componentType = arrayType.componentType
                val componentKind = componentType.kind
                when (componentKind) {
                    TypeKind.DECLARED -> (componentType as DeclaredType).getSimpleName()
                    else -> componentKind.getFieldTypeName() + "[]"
                }

            }
            else -> typeKind.getFieldTypeName()
        }
    }

    private fun DeclaredType.getSimpleName(): String {
        val typeElement = asElement() as TypeElement
        return typeElement.simpleName.toString()
    }

    private fun TypeKind.getFieldTypeName(): String {
        return when (this) {
            TypeKind.BOOLEAN -> "Boolean"
            TypeKind.INT -> "Integer"
            TypeKind.SHORT -> "Short"
            TypeKind.LONG -> "Long"
            TypeKind.FLOAT -> "Float"
            TypeKind.DOUBLE -> "Double"
            TypeKind.BYTE -> "Byte"
            TypeKind.CHAR -> "Char"
            else -> throw IllegalStateException(toString() + " should never been reached, processor error?")
        }
    }
}
