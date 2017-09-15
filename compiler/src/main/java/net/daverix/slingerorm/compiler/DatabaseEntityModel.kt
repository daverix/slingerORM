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

internal class DatabaseEntityModel(private val databaseTypeElement: TypeElement) {
    val tableName: String
        @Throws(InvalidElementException::class)
        get() {
            val annotation = databaseTypeElement.getAnnotation(DatabaseEntity::class.java) ?: throw InvalidElementException("element not annotated with @DatabaseEntity", databaseTypeElement)

            val tableName = annotation.name
            return if (tableName == "") databaseTypeElement.simpleName.toString() else tableName

        }

    val fieldNames: Array<String>
        @Throws(InvalidElementException::class)
        get() {
            return fieldsUsedInDatabase.map { it.getDatabaseFieldName() }.toTypedArray()
        }

    private val fieldsUsedInDatabase: List<Element>
        @Throws(InvalidElementException::class)
        get() = databaseTypeElement.getElements().filter { it.isDatabaseField() }

    @Throws(InvalidElementException::class)
    private fun List<Element>.getDatabaseFieldNames(): List<String> {
        return map { it.getDatabaseFieldName() }
    }

    @Throws(InvalidElementException::class)
    private fun Element.getDatabaseFieldName(): String {
        val fieldNameAnnotation = getAnnotation(FieldName::class.java) ?: return simpleName.toString()

        val fieldName = fieldNameAnnotation.value
        if (fieldName == "")
            throw InvalidElementException("fieldName must not be null or empty!", this)

        return fieldName
    }

    @Throws(InvalidElementException::class)
    fun createTableSql(): String {
        val fields = fieldsUsedInDatabase
        if (fields.isEmpty())
            throw InvalidElementException("no fields found in " + databaseTypeElement.simpleName, databaseTypeElement)

        val builder = StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(")

        val dbFieldNames = HashMap<String, String>()
        val fieldTypes = HashMap<String, String>()
        for (i in fields.indices) {
            val field = fields[i]
            val simpleName = field.simpleName.toString()
            dbFieldNames.put(simpleName, field.getDatabaseFieldName())
            fieldTypes.put(simpleName, field.getDatabaseType())
        }

        val primaryKeysCollection = primaryKeyFieldNames

        if (primaryKeysCollection.isEmpty())
            throw InvalidElementException("Primary key not found when creating SQL for entity " + databaseTypeElement.simpleName, databaseTypeElement)

        val primaryKeysCollectionSize = primaryKeysCollection.size
        for (i in fields.indices) {
            val field = fields[i]
            val fieldName = field.simpleName.toString()

            builder.append(dbFieldNames[fieldName])
                    .append(" ")
                    .append(fieldTypes[fieldName])

            val containsFieldName = primaryKeysCollection.contains(fieldName)

            if (primaryKeysCollectionSize == 1 && containsFieldName) {
                builder.append(" NOT NULL PRIMARY KEY")
            } else if (containsFieldName) {
                builder.append(" NOT NULL")
            }

            if (i < fields.size - 1) {
                builder.append(", ")
            }
        }

        if (primaryKeysCollectionSize > 1) {
            val dbNames = primaryKeysCollection.map { dbFieldNames[it] }

            builder.append(", PRIMARY KEY(")
                    .append(dbNames.joinToString(","))
                    .append(")")
        }

        builder.append(")")

        return builder.toString()
    }

    private val primaryKeyFieldNames: Set<String>
        @Throws(InvalidElementException::class)
        get() {
            val entityAnnotation = databaseTypeElement.getAnnotation(DatabaseEntity::class.java)
            val primaryKeys = entityAnnotation.primaryKeyFields
            val annotationKeys = primaryKeys
                    .filter { x -> !x.isEmpty() }
                    .toSet()

            return if (annotationKeys.isNotEmpty()) {
                annotationKeys
            } else {
                primaryKeyFields
                        .map { it.simpleName.toString() }
                        .filter { x -> !x.isEmpty() }
                        .toSet()
            }
        }

    private val primaryKeyDbNames: List<String>
        @Throws(InvalidElementException::class)
        get() = primaryKeyFields.getDatabaseFieldNames()

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
            val field = getFieldByName(key) ?: throw InvalidElementException("Field specified in DatabaseEntity annotation doesn't exist in entity class!", databaseTypeElement)

            elements.add(field)
        }

        return elements
    }

    private fun Element.isDatabaseField(): Boolean {
        if (kind != ElementKind.FIELD)
            return false

        return !modifiers.contains(Modifier.STATIC) &&
                !modifiers.contains(Modifier.TRANSIENT) &&
                getAnnotation(IgnoreField::class.java) == null
    }

    @Throws(InvalidElementException::class)
    private fun Element.getDatabaseType(): String {
        val fieldType = asType()
        val typeKind = fieldType.kind
        when (typeKind) {
            TypeKind.BOOLEAN -> return SerializeType.INT.getDatabaseType()
            TypeKind.SHORT -> return SerializeType.SHORT.getDatabaseType()
            TypeKind.LONG -> return SerializeType.LONG.getDatabaseType()
            TypeKind.INT -> return SerializeType.INT.getDatabaseType()
            TypeKind.FLOAT -> return SerializeType.FLOAT.getDatabaseType()
            TypeKind.DOUBLE -> return SerializeType.DOUBLE.getDatabaseType()
            TypeKind.DECLARED -> {
                val declaredType = fieldType as DeclaredType
                val typeElement = declaredType.asElement() as TypeElement
                val typeName = typeElement.qualifiedName.toString()

                return if (typeElement.isString()) {
                    SerializeType.STRING.getDatabaseType()
                } else {
                    val annotation = getAnnotation(SerializeTo::class.java) ?: throw InvalidElementException(typeName + " is not a type that SlingerORM understands, please add @SerializeTo and tell it what to serialize to.", this)

                    annotation.value.getDatabaseType()
                }
            }
            else -> throw InvalidElementException(simpleName.toString() + " have a type not known by SlingerORM, solve this by creating a custom serializer", this)
        }
    }

    private fun SerializeType.getDatabaseType(): String {
        return when (this) {
            SerializeType.SHORT, SerializeType.INT, SerializeType.LONG -> "INTEGER"
            SerializeType.FLOAT, SerializeType.DOUBLE -> "REAL"
            SerializeType.BYTE_ARRAY -> "BLOB"
            else -> "TEXT"
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
        return filter { item -> name == item.simpleName.toString() }.firstOrNull()
    }

    @Throws(InvalidElementException::class)
    private fun Element.findGetter(): FieldMethod {
        val objectType = getObjectTypeForElement()
        return when (objectType) {
            ObjectType.BOOLEAN,
            ObjectType.DOUBLE,
            ObjectType.FLOAT,
            ObjectType.INT,
            ObjectType.LONG,
            ObjectType.SHORT,
            ObjectType.STRING -> findDirectGetter()
            ObjectType.OTHER -> getSerializerMethod()
        }
    }

    @Throws(InvalidElementException::class)
    private fun Element.getSerializerMethod(): FieldMethod {
        val serializerFieldName = getSerializerFieldName()
        val getter = findDirectGetter()
        return WrappedFieldMethod(serializerFieldName + ".serialize(", getter, ")")
    }

    @Throws(InvalidElementException::class)
    private fun Element.getSerializerFieldName(): String {
        val typeElement = asElement()
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
    private fun Element.findDirectGetter(): FieldMethod {
        val methodsInDatabaseEntityElement = databaseTypeElement.getMethods()
        var method = findMethodByFieldNameAndGetFieldAnnotation(methodsInDatabaseEntityElement, simpleName.toString())
        if (method != null)
            return FieldMethodImpl("item." + method.simpleName + "()")

        val isBoolean = asType().kind == TypeKind.BOOLEAN
        method = methodsInDatabaseEntityElement.firstByFieldName(simpleName.toString(), if (isBoolean) "is" else "get")
        if (method != null)
            return FieldMethodImpl("item." + method.simpleName + "()")

        if (!isAccessible())
            throw InvalidElementException("No get method or a public field for " + simpleName + " in " + databaseTypeElement.simpleName,
                    databaseTypeElement)

        return FieldMethodImpl("item." + simpleName.toString())
    }

    @Throws(InvalidElementException::class)
    private fun List<ExecutableElement>.firstByFieldName(fieldName: String, prefix: String): ExecutableElement? {
        return filter { item ->
            val firstLetter = fieldName.substring(0, 1).toUpperCase()
            val methodName = prefix + firstLetter + fieldName.substring(1)

            methodName == item.simpleName.toString()
        }.firstOrNull()
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
        val typeKind = getTypeKind()
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
    private fun Element.findSetter(): FieldMethod {
        val methodsInDatabaseEntityElement = databaseTypeElement.getMethods()
        var method = methodsInDatabaseEntityElement.firstByFieldNameAndSetFieldAnnotation(simpleName.toString())
        if (method != null) {
            return method.findSetterMethodFromParameter(this)
        }

        method = methodsInDatabaseEntityElement.firstByFieldName(simpleName.toString(), "set")
        if (method != null) {
            return method.findSetterMethodFromParameter(this)
        }

        if (!isAccessible())
            throw InvalidElementException("No get method or a public field for " + simpleName + " in " + databaseTypeElement.simpleName, this)

        return WrappedFieldMethod(simpleName.toString() + " = ", findCursorMethod(this), "")
    }

    @Throws(InvalidElementException::class)
    private fun ExecutableElement.findSetterMethodFromParameter(field: Element): FieldMethod {
        if (parameters.size != 1)
            throw InvalidElementException("method has ${parameters.size} parameters, only 1 parameter supported!", this)

        return WrappedFieldMethod(simpleName.toString() + "(", parameters[0].findCursorMethod(field), ")")
    }

    @Throws(InvalidElementException::class)
    private fun Element.findCursorMethod(field: Element): FieldMethod {
        val objectType = getObjectTypeForElement()
        return if (objectType == ObjectType.OTHER) {
            findDeserializerMethod(this, field)
        } else {
            getCursorMethod(field, objectType)
        }
    }

    @Throws(InvalidElementException::class)
    private fun getCursorMethod(field: Element, objectType: ObjectType): FieldMethod {
        return when (objectType) {
            ObjectType.BOOLEAN -> FieldMethodImpl("cursor.getInt(" + getColumnIndex(field) + ") == 1")
            ObjectType.DOUBLE -> FieldMethodImpl("cursor.getDouble(" + getColumnIndex(field) + ")")
            ObjectType.FLOAT -> FieldMethodImpl("cursor.getFloat(" + getColumnIndex(field) + ")")
            ObjectType.INT -> FieldMethodImpl("cursor.getInt(" + getColumnIndex(field) + ")")
            ObjectType.LONG -> FieldMethodImpl("cursor.getLong(" + getColumnIndex(field) + ")")
            ObjectType.SHORT -> FieldMethodImpl("cursor.getShort(" + getColumnIndex(field) + ")")
            ObjectType.STRING -> FieldMethodImpl("cursor.getString(" + getColumnIndex(field) + ")")
            else -> throw UnsupportedOperationException("this should not be called!")
        }
    }

    @Throws(InvalidElementException::class)
    private fun findDeserializerMethod(element: Element, field: Element): FieldMethod {
        val serializerFieldName = field.getSerializerFieldName()
        val annotation = field.getAnnotation(SerializeTo::class.java)
        val cursorMethod = getCursorMethod(element, annotation.value.asObjectType())

        return WrappedFieldMethod(serializerFieldName + ".deserialize(", cursorMethod, ")")
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

    @Throws(InvalidElementException::class)
    private fun getColumnIndex(field: Element): String {
        return "cursor.getColumnIndex(\"${field.getDatabaseFieldName()}\")"
    }

    val setters: List<FieldMethod>
        @Throws(InvalidElementException::class)
        get() = fieldsUsedInDatabase.map { it.findSetter() }

    val getters: List<FieldMethod>
        @Throws(InvalidElementException::class)
        get() = fieldsUsedInDatabase.map { createGetter(it) }

    @Throws(InvalidElementException::class)
    private fun createGetter(field: Element?): FieldMethod {
        if (field == null) throw IllegalArgumentException("field is null")

        return WrappedFieldMethod("\"${field.getDatabaseFieldName()}\", ", field.findGetter(), "")
    }

    val itemSql: String
        @Throws(InvalidElementException::class)
        get() = primaryKeyDbNames
                .stream()
                .map { key -> "$key = ?" }
                .reduce { a, b -> "$a AND $b" }
                .orElse("")

    val itemSqlArgs: List<String>
        @Throws(InvalidElementException::class)
        get() = primaryKeyFields.map {
            val directGetter = it.findDirectGetter()
            if (it.isString()) {
                return@map directGetter.method
            } else {
                return@map "String.valueOf(${directGetter.method})"
            }
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

        val typeKind = getTypeKind()
        if (typeKind == TypeKind.DECLARED) {
            val declaredType = asType() as DeclaredType
            val typeElement = declaredType.asElement() as TypeElement
            imports.add(typeElement.qualifiedName.toString())
        }
        return imports
    }

    private fun Element.getFieldTypeName(): String {
        val typeKind = getTypeKind()
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

    private inner class WrappedFieldMethod internal constructor(private val prefix: String,
                                                                private val fieldMethod: FieldMethod,
                                                                private val suffix: String) : FieldMethod {

        override val method: String
            get() = prefix + fieldMethod.method + suffix
    }

    private inner class FieldMethodImpl internal constructor(override val method: String) : FieldMethod
}
