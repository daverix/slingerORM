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

import net.daverix.slingerorm.storage.DatabaseStorage
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * This Processor creates Implementations of interfaces annotated with [DatabaseStorage]
 */
open class DatabaseStorageProcessor : AbstractProcessor() {
    override fun process(typeElements: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        val dbEntityModels = DatabaseEntityModelLazyMap()

        for (entity in roundEnvironment.getElementsAnnotatedWith(DatabaseStorage::class.java)) {
            try {
                val element = entity as TypeElement
                val packageName = element.packageName
                val interfaceName = element.typeName
                val storageName = element.simpleName.toString()

                buildStorage(packageName, interfaceName, storageName) {
                    accept(CreateTableVisitor(element, processingEnv.typeUtils, dbEntityModels))
                    accept(DeleteVisitor(element, processingEnv.typeUtils, dbEntityModels))
                    accept(SelectVisitor(element, processingEnv.typeUtils, dbEntityModels))
                    accept(ReplaceVisitor(element, dbEntityModels))
                    accept(InsertVisitor(element, dbEntityModels))
                    accept(UpdateVisitor(element, dbEntityModels))
                }.writeTo(processingEnv.filer)
            } catch (e: IOException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.localizedMessage)
            } catch (e: InvalidElementException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.message, e.element)
            } catch (e: Exception) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Internal error: " + e.getStackTraceString(""))
            }
        }
        return true // no further processing of this annotation type
    }
}
