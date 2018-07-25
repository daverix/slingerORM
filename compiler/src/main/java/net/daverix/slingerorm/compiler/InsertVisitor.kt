package net.daverix.slingerorm.compiler

import com.squareup.javapoet.MethodSpec
import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.Insert
import java.lang.IllegalArgumentException
import javax.lang.model.element.TypeElement


class InsertVisitor(private val typeElement: TypeElement,
                    private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {
    override fun visit(builder: DatabaseStorageBuilder) {
        val insertMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<Insert>() }
                .toList()

        builder.methods += insertMethods.map {
            if (it.parameters.size > 1)
                throw InvalidElementException("Only one parameter supported for Insert methods", it)

            if (it.parameters.size == 0)
                throw InvalidElementException("Insert methods must have one parameter which is the entity to insert", it)

            val firstParameter = it.parameters.first()
            val databaseEntityElement = firstParameter.asTypeElement()
            if (!databaseEntityElement.isAnnotatedWith<DatabaseEntity>())
                throw InvalidElementException("The type of parameter ${firstParameter.simpleName} must be annotated with @DatabaseEntity", firstParameter)

            val model = dbEntities[databaseEntityElement]

            MethodSpec.overriding(it)
                    .apply {
                        beginControlFlow("if (${firstParameter.simpleName} == null)")
                                .addStatement("throw new \$T(\$S)",
                                        IllegalArgumentException::class.java,
                                        "${firstParameter.simpleName} is null")
                                .endControlFlow()

                        addCode("\$1T values = new \$1T();\n", ClassNames.CONTENT_VALUES)

                        for (getter in model.getGetters(firstParameter.simpleName.toString())) {
                            addCode("values.put($getter);\n")
                        }

                        //TODO: check return type so that rowId from SQLite can be returned?
                        //TODO: should we set the id on the entity if the table is autoIncrementing?
                        addCode("db.insertOrThrow(\"${model.tableName}\", null, values);\n")
                    }
                    .build()
        }.toList()
    }
}