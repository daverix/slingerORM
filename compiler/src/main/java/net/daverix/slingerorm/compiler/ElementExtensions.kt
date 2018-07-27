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

import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass


const val TYPE_STRING = "java.lang.String"

val Element.typeKind: TypeKind get() = asType().kind

val Element.typeKindName: String
    get() = when (typeKind) {
        TypeKind.INT -> "int"
        TypeKind.SHORT -> "short"
        TypeKind.LONG -> "long"
        TypeKind.FLOAT -> "float"
        TypeKind.DOUBLE -> "double"
        TypeKind.CHAR -> "char"
        TypeKind.BYTE -> "byte"
        TypeKind.BOOLEAN -> "boolean"
        TypeKind.DECLARED -> {
            val typeElement = (asType() as DeclaredType).asElement() as TypeElement
            typeElement.simpleName.toString()
        }
        else -> throw InvalidElementException(typeKind.toString() + " is not known, bug?", this)
    }

val Element.isAccessible: Boolean
    get() {
        return Modifier.TRANSIENT !in modifiers &&
                Modifier.PROTECTED !in modifiers &&
                Modifier.STATIC !in modifiers &&
                Modifier.PRIVATE !in modifiers
    }

@Throws(InvalidElementException::class)
fun Element.asTypeElement(): TypeElement {
    if (typeKind != TypeKind.DECLARED)
        throw InvalidElementException("Element is not a declared type: $typeKind", this)

    if (asType() !is DeclaredType) {
        throw InvalidElementException("mirrorType expected to be DeclaredType but was " + asType(), this)
    }

    val declaredType = asType() as DeclaredType
    return declaredType.asElement() as TypeElement
}

val Element.qualifiedMapperName: String
    get() {
        return asTypeElement().qualifiedName.toString()
    }

val TypeElement.elements: List<Element>
    get() {
        val elements = ArrayList<Element>()
        val visitedTypes = HashSet<String>()
        addElements(elements, this, visitedTypes)
        return elements
    }

private fun addElements(elements: MutableList<Element>,
                        entity: TypeElement,
                        names: MutableSet<String>) {
    for (element in entity.enclosedElements) {
        val name = element.simpleName.toString()
        if (!names.contains(name)) {
            elements.add(element)
            names.add(name)
        }
    }

    val parentMirror = entity.superclass
    if (parentMirror.kind == TypeKind.DECLARED) {
        val declaredType = parentMirror as DeclaredType
        addElements(elements, declaredType.asElement() as TypeElement, names)
    }
}

val TypeElement.methods: List<ExecutableElement>
    get() {
        return elements.filter { it.kind == ElementKind.METHOD && it.isAccessible }
                .map { it as ExecutableElement }
    }

val TypeElement.directMethods: List<ExecutableElement>
    get() {
        return enclosedElements.filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
    }

val TypeElement.packageName: String
    get() {
        return qualifiedName.substring(0, qualifiedName.lastIndexOf("."))
    }

data class AnnotationType<T : Annotation>(val type: KClass<T>)

inline fun <reified T : Annotation> annotation() = AnnotationType(T::class)

fun Element.isAnnotatedWithAnyOf(vararg annotations: AnnotationType<*>) = annotations.any {
    getAnnotation(it.type.java) != null
}

inline fun <reified T : Annotation> Element.isAnnotatedWith() = getAnnotation(T::class.java) != null

inline fun <reified T> Element.getAnnotationAttribute(attributeName: String): TypeMirror? {
    return annotationMirrors.firstOrNull { it.annotationType.toString() == T::class.java.name }
            ?.elementValues?.entries?.filter { it.key.simpleName.toString() == attributeName }
            ?.map { it.value.value as TypeMirror }
            ?.firstOrNull()
}

fun Element.isString(): Boolean {
    val elementType = asType()
    if(elementType.kind != TypeKind.DECLARED) return false

    val declaredType = elementType as DeclaredType
    val typeElement = declaredType.asElement() as TypeElement
    return typeElement.qualifiedName.toString() == TYPE_STRING
}

fun VariableElement.getParameterVariable(): String = when (typeKind) {
    TypeKind.BOOLEAN -> "$simpleName ? \"1\" : \"0\""
    TypeKind.DECLARED -> {
        simpleName.toString().encloseStringValueOfIfNotString(isString())
    }
    else -> "String.valueOf($simpleName)"
}
