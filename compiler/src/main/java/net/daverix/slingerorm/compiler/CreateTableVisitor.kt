package net.daverix.slingerorm.compiler

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import net.daverix.slingerorm.storage.CreateTable
import java.util.*
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.util.Types

class CreateTableVisitor(private val typeElement: TypeElement,
                         private val typeUtils: Types,
                         private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {
    override fun visit(builder: DatabaseStorageBuilder) {
        val createTableMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<CreateTable>() }
                .toList()

        val fieldNames = createTableMethods
                .map { it.getAnnotation(CreateTable::class.java) }
                .map { it.getTypeElement() }
                .distinct()
                .associateBy({it}, {"${it.simpleName.toString().fromCamelCaseToScreamingSnakeCase()}_CREATE_TABLE_SQL"})

        builder.fields += fieldNames.map { createSqlField(it.key, it.value)}

        builder.methods += createTableMethods.map {
            val annotation = it.getAnnotation(CreateTable::class.java)
            val entity = annotation.getTypeElement()
            MethodSpec.overriding(it)
                    .addStatement("db.execSQL(\$L)", fieldNames[entity])
                    .build()
        }.toList()
    }

    private fun createSqlField(it: TypeElement, name: String): FieldSpec =
            FieldSpec.builder(TypeName.get(String::class.java), name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\$S", createTableSql(it))
                    .build()

    private fun CreateTable.getTypeElement(): TypeElement {
        try {
            value
            throw IllegalStateException("should never reach this line (this is a hack)")
        } catch (mte: MirroredTypeException) {
            return typeUtils.asElement(mte.typeMirror) as TypeElement
        }
    }

    @Throws(InvalidElementException::class)
    private fun createTableSql(databaseTypeElement: TypeElement): String {
        val databaseEntityModel = dbEntities[databaseTypeElement]
        val fields = databaseEntityModel.fieldsUsedInDatabase
        if (fields.isEmpty())
            throw InvalidElementException("no fields found in ${databaseTypeElement.simpleName}", databaseTypeElement)

        val builder = StringBuilder("CREATE TABLE IF NOT EXISTS ").append(databaseEntityModel.tableName).append("(")

        val dbFieldNames = HashMap<String, String>()
        val fieldTypes = HashMap<String, String>()
        for (i in fields.indices) {
            val field = fields[i]
            val simpleName = field.simpleName.toString()
            dbFieldNames[simpleName] = databaseEntityModel.getDatabaseFieldName(field)
            fieldTypes[simpleName] = databaseEntityModel.getDatabaseType(field)
        }

        val primaryKeysCollection = databaseEntityModel.primaryKeyFieldNames

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
}
