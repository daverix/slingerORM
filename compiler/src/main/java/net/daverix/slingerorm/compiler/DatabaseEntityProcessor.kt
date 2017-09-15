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

import net.daverix.slingerorm.entity.DatabaseEntity
import java.io.BufferedWriter
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */

open class DatabaseEntityProcessor : AbstractProcessor() {
    private lateinit var packageProvider: PackageProvider

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        packageProvider = PackageProvider()
    }

    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        val entities = roundEnvironment.getElementsAnnotatedWith(DatabaseEntity::class.java)
                .filter { !it.modifiers.contains(Modifier.ABSTRACT) }

        for (entity in entities) {
            try {
                createMapper(entity as TypeElement)
            } catch (e: IOException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: ${e.localizedMessage}")
            } catch (e: InvalidElementException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: ${e.message}", e.element)
            } catch (e: Exception) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Internal error: ${e.getStackTraceString()}")
            }
        }
        return true
    }

    @Throws(IOException::class, InvalidElementException::class)
    private fun createMapper(entity: TypeElement) {
        val model = DatabaseEntityModel(entity)
        val mapperName = "${entity.simpleName}Mapper"
        val packageName = packageProvider.getPackage(entity.qualifiedName.toString())

        val jfo = processingEnv.filer.createSourceFile(packageName + "." + mapperName)
        BufferedWriter(jfo.openWriter()).use { bw ->
            val builder = DatabaseEntityMapperBuilder.builder(bw)
            builder.apply {
                databaseEntityClassName = (entity.simpleName.toString())
                this.packageName = packageName
                tableName = model.tableName
                createTableSql = model.createTableSql()
                fieldNames += model.fieldNames
                setters += model.setters
                getters += model.getters
                itemSql = model.itemSql
                itemSqlArguments += model.itemSqlArgs
                serializers += model.serializers

                build()
            }
        }
    }
}
