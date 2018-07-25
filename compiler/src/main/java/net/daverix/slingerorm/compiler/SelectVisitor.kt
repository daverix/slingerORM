package net.daverix.slingerorm.compiler

import com.squareup.javapoet.MethodSpec
import net.daverix.slingerorm.DataPointer
import net.daverix.slingerorm.entity.DatabaseEntity
import net.daverix.slingerorm.storage.Select
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Types


class SelectVisitor(private val typeElement: TypeElement,
                    private val typeUtils: Types,
                    private val dbEntities: DatabaseEntityModelMap) : DatabaseStorageBuilderVisitor {

    override fun visit(builder: DatabaseStorageBuilder) {
        val selectMethods = typeElement.directMethods
                .filter { it.isAnnotatedWith<Select>() }
                .toList()

        builder.methods += selectMethods.map {
            when (it.returnType.kind) {
                TypeKind.ARRAY -> createMethodForArrayType(it)
                TypeKind.DECLARED -> createMethodForDeclaredType(it)
                else -> failReturnType(it)
            }
        }.toList()
    }

    private fun failReturnType(methodElement: ExecutableElement): Nothing {
        throw InvalidElementException("Select methods must return either a collection of entities or a single entity ${methodElement.returnType}", methodElement)
    }

    private fun createSelectSingleMethod(methodElement: ExecutableElement,
                                         returnTypeElement: TypeElement): MethodSpec =
            MethodSpec.overriding(methodElement)
                    .apply {
                        val model = dbEntities[returnTypeElement]
                        val fields = model.fieldNames.joinToString(",\n") { "    \"$it\"" }

                        addCode("\$T pointer = db.query(false,\n", DataPointer::class.java)
                        addCode("  \"${model.tableName}\",\n")
                        addCode("  new String[] {\n")
                        addCode(fields)
                        addCode("  },\n")
                        addCode("  \"${model.itemSql}\",\n")
                        addCode("  new String[] {\n")
                        addVariableArrayCode(methodElement.parameters, "    ")
                        addCode("  },\n")
                        addCode("  null,\n")
                        addCode("  null,\n")
                        addCode("  null,\n")
                        addCode("  null);\n")
                        addCode("\n")

                        tryFinally({
                            beginControlFlow("if(pointer.moveToFirst())")
                                addStatement("\$T entity = new \$T()", returnTypeElement.asType(), returnTypeElement.asType())
                                addSetterStatements(model, "entity", "pointer")
                                addStatement("return entity")
                            nextControlFlow("else")
                                addStatement("return null")
                            endControlFlow()
                        }, {
                            addStatement("pointer.close()")
                        })
                    }
                    .build()



    private fun MethodSpec.Builder.addSetterStatements(model: DatabaseEntityModel,
                                                       entityVariableName: String,
                                                       pointerVariableName: String) {
        for (setter in model.getSetters(pointerVariableName)) {
            addStatement("$entityVariableName.$setter")
        }
    }

    private fun createMethodForDeclaredType(methodElement: ExecutableElement): MethodSpec {
        val returnTypeElement = (methodElement.returnType as DeclaredType).asElement() as TypeElement
        val returnTypeName = returnTypeElement.qualifiedName.toString()
        return when {
            returnTypeName in SUPPORTED_RETURN_TYPES_FOR_SELECT_MULTIPLE -> createMethodForCollectionType(returnTypeElement, methodElement)
            returnTypeElement.isAnnotatedWith<DatabaseEntity>() -> createSelectSingleMethod(methodElement, returnTypeElement)
            else -> failReturnType(methodElement)
        }
    }

    private fun createMethodForCollectionType(returnTypeElement: TypeElement, methodElement: ExecutableElement): MethodSpec {
        val firstTypeArgument = (methodElement.returnType as DeclaredType).typeArguments.first()
        if (firstTypeArgument.kind != TypeKind.DECLARED)
            throw InvalidElementException("Returned type ${methodElement.returnType} with type parameter $firstTypeArgument must be annotated with @DatabaseEntity", methodElement)

        val parameterTypeElement = typeUtils.asElement(firstTypeArgument) as TypeElement
        return MethodSpec.overriding(methodElement)
                .apply {

                    addCode("return null;\n")
                }
                .build()
    }

    private fun createMethodForArrayType(methodElement: ExecutableElement): MethodSpec {
        val componentType = typeUtils.getArrayType(methodElement.returnType).componentType
        if (componentType.kind != TypeKind.DECLARED)
            failArrayNotHavingDatabaseEntityAnnotatedType(methodElement)

        val typeElement = (typeUtils.asElement(componentType) as TypeElement)
        if (!typeElement.isAnnotatedWith<DatabaseEntity>())
            failArrayNotHavingDatabaseEntityAnnotatedType(methodElement)

        return MethodSpec.overriding(methodElement)
                .apply {

                    addCode("return null;\n")
                }
                .build()
    }

    private fun failArrayNotHavingDatabaseEntityAnnotatedType(it: ExecutableElement): Nothing {
        throw InvalidElementException("Returned array must contain a type that is annotated with @DatabaseEntity", it)
    }

    companion object {
        private val SUPPORTED_RETURN_TYPES_FOR_SELECT_MULTIPLE = listOf(
                "java.util.List",
                "java.util.Collection",
                "java.lang.Iterable"
        )
    }
}