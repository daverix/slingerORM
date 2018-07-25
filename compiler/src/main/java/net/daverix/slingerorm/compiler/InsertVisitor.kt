package net.daverix.slingerorm.compiler

import com.squareup.javapoet.MethodSpec
import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.Insert
import javax.lang.model.element.TypeElement


class InsertVisitor(private val typeElement: TypeElement,
                    private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {
    override fun visit(builder: DatabaseStorageBuilder) {
        val insertMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<Insert>() }
                .toList()

        builder.methods += insertMethods.map {
            if(it.parameters.size > 1)
                throw InvalidElementException("Only one parameter supported for Insert methods", it)

            if(it.parameters.size == 0)
                throw InvalidElementException("Insert methods must have one parameter which is the entity to insert", it)

            val firstParameter = it.parameters.first()
            val databaseEntityElement = firstParameter.asTypeElement()
            if(!databaseEntityElement.isAnnotatedWith<DatabaseEntity>())
                throw InvalidElementException("The type of parameter ${firstParameter.simpleName} must be annotated with @DatabaseEntity", firstParameter)

            val model = dbEntities[databaseEntityElement]

            MethodSpec.overriding(it)
                    .apply {
                        addCode("db.edit(\"${model.tableName}\")\n")
                        for(getter in model.getGetters(firstParameter.simpleName.toString())) {
                            addCode("  .put($getter)\n")
                        }
                        addCode("  .insert();\n")
                    }
                    .build()
        }.toList()
    }
}