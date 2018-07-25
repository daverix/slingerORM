package net.daverix.slingerorm.compiler

import com.squareup.javapoet.*
import net.daverix.slingerorm.Database
import java.lang.IllegalArgumentException
import javax.lang.model.element.Modifier

interface DatabaseStorageBuilderVisitor {
    fun visit(builder: DatabaseStorageBuilder)
}

fun buildStorage(packageName: String,
                 interfaceName: TypeName,
                 storageName: String,
                 func: DatabaseStorageBuilder.()->Unit): JavaFile {
    val builder = DatabaseStorageBuilder(packageName, interfaceName, storageName)
    func(builder)
    return builder.build()
}

class DatabaseStorageBuilder internal constructor(private val packageName: String,
                                                  private val interfaceName: TypeName,
                                                  private val storageName: String) {
    val fields = ArrayList<FieldSpec>()
    val methods = ArrayList<MethodSpec>()

    fun accept(visitor: DatabaseStorageBuilderVisitor): DatabaseStorageBuilder {
        visitor.visit(this)
        return this
    }

    fun build(): JavaFile = JavaFile.builder(packageName, createStorageClass()).build()

    private fun createStorageClass(): TypeSpec {
        val implementationName = "Slinger$storageName"
        val storageClassName = ClassName.get(packageName, implementationName)
        val builderClassName = ClassName.get(packageName, implementationName,"Builder")

        val databaseFieldName = "db"
        val databaseField = createDatabaseField(databaseFieldName)

        val constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(builderClassName, "builder").build())
                .addFieldAssignStatement("db", "builder")
                .build()

        val builderMethod = MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderClassName)
                .addStatement("return new \$T()", builderClassName)
                .build()

        val storageBuilderClass = createStorageBuilderClass(builderClassName, storageClassName)

        return TypeSpec.classBuilder(storageClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceName)
                .addField(databaseField)
                .addFields(fields)
                .addMethod(constructor)
                .addMethods(methods)
                .addMethod(builderMethod)
                .addType(storageBuilderClass)
                .build()
    }

    private fun createStorageBuilderClass(builderClassName: ClassName,
                                          storageClassName: ClassName): TypeSpec {
        val databaseFieldName = "db"
        val databaseField = FieldSpec.builder(Database::class.java, databaseFieldName)
                .addModifiers(Modifier.PRIVATE)
                .build()

        val constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build()

        val databaseMethod = createSetterMethod("database",
                "db",
                "db",
                ClassName.get(Database::class.java),
                builderClassName)

        val buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceName)
                .addFieldNotSetStatement("db", "database")
                .addStatement("return new \$T(this)", storageClassName)
                .build()

        return TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addField(databaseField)
                .addMethod(constructor)
                .addMethod(databaseMethod)
                .addMethod(buildMethod)
                .build()
    }

    private fun createDatabaseField(databaseFieldName: String): FieldSpec? {
        return FieldSpec.builder(Database::class.java, databaseFieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build()
    }

    private fun MethodSpec.Builder.addFieldAssignStatement(
            fieldName: String,
            parameterName: String): MethodSpec.Builder {
        return addStatement("this.$fieldName = $parameterName.$fieldName")
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
}