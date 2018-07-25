package net.daverix.slingerorm.compiler

import com.squareup.javapoet.MethodSpec
import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.Replace
import javax.lang.model.element.TypeElement


class ReplaceVisitor(private val typeElement: TypeElement,
                     private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {
    override fun visit(builder: DatabaseStorageBuilder) {
        val replaceMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<Replace>() }
                .toList()

        builder.methods += replaceMethods.map {
            if(it.parameters.size > 1)
                throw InvalidElementException("Only one parameter supported for Replace methods", it)

            if(it.parameters.size == 0)
                throw InvalidElementException("Replace methods must have one parameter which is the entity to replace", it)

            val firstParameter = it.parameters.first()
            val databaseEntityElement = firstParameter.asTypeElement()
            if(!databaseEntityElement.isAnnotatedWith<DatabaseEntity>())
                throw InvalidElementException("The type of parameter ${firstParameter.simpleName} must be annotated with @DatabaseEntity", firstParameter)

            val model = dbEntities[databaseEntityElement]

            MethodSpec.overriding(it)
                    .apply {
                        addStatement("if (${firstParameter.simpleName} == null) throw new \$T(\"${firstParameter.simpleName} is null\")", IllegalArgumentException::class.java)
                        addCode("db.edit(\"${model.tableName}\")\n")

                        for(getter in model.getGetters(firstParameter.simpleName.toString())) {
                            addCode("  .put($getter)\n")
                        }
                        addCode("  .replace();\n")
                    }
                    .build()
        }.toList()
    }
}