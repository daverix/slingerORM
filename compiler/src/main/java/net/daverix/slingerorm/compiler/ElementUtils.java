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
package net.daverix.slingerorm.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

final class ElementUtils {
    static final String TYPE_STRING = "java.lang.String";

    private ElementUtils() {
    }

    static <T extends Element> List<T> filter(List<T> items, ElementPredicate<T> predicate) throws InvalidElementException {
        List<T> filtered = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (predicate.test(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    static <T extends Element, R> List<R> map(List<T> items, ElementFunction<T, R> function) throws InvalidElementException {
        List<R> mapped = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            mapped.add(function.apply(items.get(i)));
        }
        return mapped;
    }

    static TypeKind getTypeKind(Element element) {
        final TypeMirror fieldType = element.asType();
        return fieldType.getKind();
    }

    static boolean isAccessible(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        return !modifiers.contains(Modifier.TRANSIENT) &&
                !modifiers.contains(Modifier.PROTECTED) &&
                !modifiers.contains(Modifier.STATIC) &&
                !modifiers.contains(Modifier.PRIVATE);
    }

    static String getDeclaredTypeName(Element element) throws InvalidElementException {
        if (element == null) throw new IllegalArgumentException("element is null");
        return getTypeElement(element).getQualifiedName().toString();
    }

    static TypeElement getTypeElement(Element element) throws InvalidElementException {
        if (element == null) throw new IllegalArgumentException("element is null");

        TypeKind elementTypeKind = getTypeKind(element);
        if (elementTypeKind != TypeKind.DECLARED)
            throw new InvalidElementException("Element is not a declared type: " + elementTypeKind, element);

        if (!(element.asType() instanceof DeclaredType)) {
            throw new InvalidElementException("mirrorType expected to be DeclaredType but was " + element.asType(), element);
        }

        final DeclaredType declaredType = (DeclaredType) element.asType();
        return (TypeElement) declaredType.asElement();
    }

    static List<Element> getElementsInTypeElement(TypeElement entity) {
        List<Element> elements = new ArrayList<>();
        Set<String> visitedTypes = new HashSet<>();
        addElements(elements, entity, visitedTypes);
        return elements;
    }

    private static void addElements(List<Element> elements, TypeElement entity, Set<String> names) {
        for (Element element : entity.getEnclosedElements()) {
            String name = element.getSimpleName().toString();
            if (!names.contains(name)) {
                elements.add(element);
                names.add(name);
            }
        }

        TypeMirror parentMirror = entity.getSuperclass();
        if (parentMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) parentMirror;
            addElements(elements, (TypeElement) declaredType.asElement(), names);
        }
    }

    static List<ExecutableElement> getMethodsInTypeElement(TypeElement typeElement) throws InvalidElementException {
        return map(filter(getElementsInTypeElement(typeElement),
                item -> item.getKind() == ElementKind.METHOD && isAccessible(item)),
                item -> (ExecutableElement) item);
    }

    static boolean isString(Element element) {
        TypeKind typeKind = getTypeKind(element);
        if (typeKind == TypeKind.DECLARED) {
            final DeclaredType declaredType = (DeclaredType) element.asType();
            final TypeElement typeElement = (TypeElement) declaredType.asElement();
            final String typeName = typeElement.getQualifiedName().toString();
            return typeName.equals(ElementUtils.TYPE_STRING);
        }

        return false;
    }
}
