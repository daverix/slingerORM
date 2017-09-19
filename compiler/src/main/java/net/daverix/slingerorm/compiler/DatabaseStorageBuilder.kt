package net.daverix.slingerorm.compiler

import com.squareup.javapoet.*
import net.daverix.slingerorm.Database
import java.lang.IllegalArgumentException
import javax.lang.model.element.Modifier


class DatabaseStorageBuilder(private val packageName: String,
                             private val interfaceName: TypeName,
                             private val storageName: String) {
    var mappers = emptyList<MapperInfo>()
    var methods = emptyList<MethodSpec>()

    fun build(): JavaFile {
        val storageClassName = ClassName.get(packageName, storageName)
        val builderClassName = ClassName.get(packageName, storageName,"Builder")

        val storageBuilderClass = createStorageBuilderClass(builderClassName, storageClassName,
                mappers)
        val storageClass = createStorageClass(storageClassName, interfaceName, builderClassName,
                mappers, methods, storageBuilderClass)
        return JavaFile.builder(packageName, storageClass).build()
    }

    private fun createStorageClass(className: ClassName,
                                   interfaceName: TypeName,
                                   builderName: ClassName,
                                   mappers: List<MapperInfo>,
                                   methods: List<MethodSpec>,
                                   builder: TypeSpec): TypeSpec {
        val databaseFieldName = "db"
        val databaseField = FieldSpec.builder(Database::class.java, databaseFieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build()
        val mapperFields = mappers.map { createMapperField(it, Modifier.PRIVATE, Modifier.FINAL) }

        val constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(builderName, "builder").build())
                .addFieldAssignStatement("db", "builder")
                .addMapperFieldsAssignments(mappers)
                .build()

        val builderMethod = MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderName)
                .addStatement("return new \$T()", builderName)
                .build()

        return TypeSpec.classBuilder(className)
                .addSuperinterface(interfaceName)
                .addField(databaseField)
                .addFields(mapperFields)
                .addMethod(constructor)
                .addMethods(methods)
                .addMethod(builderMethod)
                .addType(builder)
                .build()
    }

    private fun MethodSpec.Builder.addMapperFieldsAssignments(
            mappers: List<MapperInfo>): MethodSpec.Builder {
        mappers.forEach { addFieldAssignStatement(it.variableName, "builder") }
        return this
    }

    private fun MethodSpec.Builder.addFieldAssignStatement(
            fieldName: String,
            parameterName: String): MethodSpec.Builder {
        return addStatement("this.$fieldName = $parameterName.$fieldName")
    }

    private fun createStorageBuilderClass(builderClassName: ClassName,
                                          storageClassname: ClassName,
                                          mappers: List<MapperInfo>): TypeSpec {
        val databaseFieldName = "db"
        val databaseField = FieldSpec.builder(Database::class.java, databaseFieldName)
                .addModifiers(Modifier.PRIVATE)
                .build()
        val mapperFields = mappers.map { createMapperField(it, Modifier.PRIVATE) }

        val constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build()

        val databaseMethod = createSetterMethod("database",
                "db",
                "db",
                ClassName.get(Database::class.java),
                builderClassName)

        val mapperMethods = mappers.map { createMapperMethod(it, builderClassName) }

        val buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(storageClassname)
                .addFieldNotSetStatement("db", "database")
                .addMappersBuildStatements(mappers)
                .addStatement("return new \$T(this)", storageClassname)
                .build()

        return TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addField(databaseField)
                .addFields(mapperFields)
                .addMethod(constructor)
                .addMethod(databaseMethod)
                .addMethods(mapperMethods)
                .addMethod(buildMethod)
                .build()
    }

    private fun MethodSpec.Builder.addMappersBuildStatements(
            mappers: Iterable<MapperInfo>): MethodSpec.Builder {
        mappers.forEach {
            if (it.hasDependencies)
                addFieldNotSetStatement(it.variableName, it.variableName)
            else {
                beginControlFlow("if (${it.variableName} == null)")
                        .addStatement("this.${it.variableName} = \$T.create()", it.typeName)
                        .endControlFlow()
            }
        }

        return this
    }

    private fun createSetterMethod(methodName: String,
                                   fieldName: String,
                                   parameterName: String,
                                   parameterType: TypeName,
                                   returnType: TypeName): MethodSpec {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(parameterType, parameterName).build())
                .returns(returnType)
                .beginControlFlow("if ($parameterName == null)")
                .addStatement("throw new \$T(\$S)",
                        IllegalArgumentException::class.java,
                        "$parameterName is null")
                .endControlFlow()
                .addStatement("this.\$L = \$L;", fieldName, parameterName)
                .addStatement("return this")
                .build()
    }

    private fun MethodSpec.Builder.addFieldNotSetStatement(fieldName: String, name: String): MethodSpec.Builder {
        return beginControlFlow("if ($fieldName == null)")
                .addStatement("throw new \$T(\$S)",
                        IllegalArgumentException::class.java,
                        "$name is not set")
                .endControlFlow()
    }

    private fun createMapperMethod(mapper: MapperInfo, builderName: TypeName): MethodSpec {
        return createSetterMethod(mapper.variableName,
                mapper.variableName,
                mapper.variableName,
                mapper.interfaceTypeName,
                builderName)
    }

    private fun createMapperField(mapperInfo: MapperInfo, vararg modifiers: Modifier): FieldSpec {
        return FieldSpec.builder(mapperInfo.interfaceTypeName, mapperInfo.variableName)
                .addModifiers(*modifiers)
                .build()
    }
}