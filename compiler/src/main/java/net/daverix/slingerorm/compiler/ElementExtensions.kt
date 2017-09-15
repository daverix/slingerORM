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


const val TYPE_STRING = "java.lang.String"

fun Element.getTypeKind(): TypeKind {
    return asType().kind
}

fun Element.isAccessible(): Boolean {
    return !modifiers.contains(Modifier.TRANSIENT) &&
            !modifiers.contains(Modifier.PROTECTED) &&
            !modifiers.contains(Modifier.STATIC) &&
            !modifiers.contains(Modifier.PRIVATE)
}

@Throws(InvalidElementException::class)
fun Element.getQualifiedMapperName(): String {
    return asElement().qualifiedName.toString()
}

@Throws(InvalidElementException::class)
fun Element.asElement(): TypeElement {
    val elementTypeKind = getTypeKind()
    if (elementTypeKind != TypeKind.DECLARED)
        throw InvalidElementException("Element is not a declared type: " + elementTypeKind, this)

    if (asType() !is DeclaredType) {
        throw InvalidElementException("mirrorType expected to be DeclaredType but was " + asType(), this)
    }

    val declaredType = asType() as DeclaredType
    return declaredType.asElement() as TypeElement
}

fun TypeElement.getElements(): List<Element> {
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

@Throws(InvalidElementException::class)
fun TypeElement.getMethods(): List<ExecutableElement> {
    return getElements()
            .filter { it.kind == ElementKind.METHOD && it.isAccessible() }
            .map { it as ExecutableElement }
            .toList()
}

fun Element.isString(): Boolean {
    return if (getTypeKind() == TypeKind.DECLARED) {
        val declaredType = asType() as DeclaredType
        val typeElement = declaredType.asElement() as TypeElement
        typeElement.qualifiedName.toString() == TYPE_STRING
    } else false
}

@Throws(InvalidElementException::class)
fun Element.getTypeName(): String {
    val typeKind = asType().kind
    return when (typeKind) {
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
}

