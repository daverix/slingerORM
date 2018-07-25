package net.daverix.slingerorm.compiler

import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.Delete
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Types


class DeleteVisitor(private val typeElement: TypeElement,
                    private val typeUtils: Types,
                    private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {

    override fun visit(builder: DatabaseStorageBuilder) {
        val deleteMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<Delete>() }
                .toList()

        builder.methods += deleteMethods.map {
            if (it.returnType.kind !in setOf(TypeKind.VOID, TypeKind.INT))
                throw InvalidElementException("Return type ${it.returnType} is unsupported, use void or int", it)

            val prefix = if (it.returnType.kind == TypeKind.INT) "return " else ""

            val deleteAnnotation = it.getAnnotation(Delete::class.java)
            val deleteAnnotationType = deleteAnnotation!!.getTypeElement()
            val deleteAnnotationTypeQualifiedName = deleteAnnotationType.qualifiedName.toString()

            if (deleteAnnotationTypeQualifiedName != "java.lang.Object") {
                val model = dbEntities[deleteAnnotationType]
                if (model.primaryKeyFieldNames.size != it.parameters.size)
                    throw InvalidElementException("Parameters does not match the primary keys ${model.primaryKeyFieldNames.joinToString()} in ${model.tableName}", it)

                it.toMethodSpec {
                    addCode("${prefix}db.delete(\"${model.tableName}\",\"${model.itemSql}\", new String[] {\n")
                    addVariableArrayCode(it.parameters, "  ")
                    addCode("});\n")
                }
            } else {
                val firstParameter = it.parameters.first()
                val databaseEntityElement = firstParameter.asTypeElement()

                val firstParameterTypeAnnotated = databaseEntityElement.isAnnotatedWith<DatabaseEntity>()
                if (firstParameterTypeAnnotated && it.parameters.size != 1)
                    throw InvalidElementException("Only one parameter supported when passing a @DatabaseEntity annotated type and not having the Delete annotation set to a specific entity type", it)

                val model = dbEntities[databaseEntityElement]

                it.toMethodSpec {
                    addCode("if (${firstParameter.simpleName} == null) throw new \$T(\"${firstParameter.simpleName} is null\");\n\n", IllegalArgumentException::class.java)

                    addCode("${prefix}db.delete(\"${model.tableName}\",\"${model.itemSql}\", new String[] {\n")
                    addCode(model.getItemSqlArgs(firstParameter.simpleName.toString()).joinToString(",") { "  $it\n" })
                    addCode("});\n")
                }
            }
        }.toList()
    }

    private fun Delete.getTypeElement(): TypeElement {
        try {
            value
            throw IllegalStateException("should never reach this line (this is a hack)")
        } catch (mte: MirroredTypeException) {
            return typeUtils.asElement(mte.typeMirror) as TypeElement
        }
    }
}
