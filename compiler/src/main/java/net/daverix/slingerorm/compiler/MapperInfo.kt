package net.daverix.slingerorm.compiler

import com.squareup.javapoet.TypeName

data class MapperInfo(val variableName: String,
                      val interfaceTypeName: TypeName,
                      val typeName: TypeName,
                      val entityTypeName: TypeName,
                      val hasDependencies: Boolean)