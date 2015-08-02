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

import static net.daverix.slingerorm.compiler.ListUtils.filter;
import static net.daverix.slingerorm.compiler.ListUtils.mapItems;

final class ElementUtils {
    public static final String TYPE_STRING = "java.lang.String";
    public static final String TYPE_DATE = "java.util.Date";
    public static final String TYPE_INTEGER = "java.lang.Integer";
    public static final String TYPE_SHORT = "java.lang.Short";
    public static final String TYPE_LONG = "java.lang.Long";
    public static final String TYPE_FLOAT = "java.lang.Float";
    public static final String TYPE_DOUBLE = "java.lang.Double";
    public static final String TYPE_BOOLEAN = "java.lang.Boolean";

    private ElementUtils(){}

    public static TypeKind getTypeKind(Element element) {
        final TypeMirror fieldType = element.asType();
        return fieldType.getKind();
    }

    public static boolean isAccessible(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        return !modifiers.contains(Modifier.TRANSIENT) &&
                !modifiers.contains(Modifier.PROTECTED) &&
                !modifiers.contains(Modifier.STATIC) &&
                !modifiers.contains(Modifier.PRIVATE);
    }

    public static String getDeclaredTypeName(Element element) throws InvalidElementException {
        if(element == null) throw new IllegalArgumentException("element is null");
        return getTypeElement(element).getQualifiedName().toString();
    }

    public static TypeElement getTypeElement(Element element) throws InvalidElementException {
        if(element == null) throw new IllegalArgumentException("element is null");

        TypeKind elementTypeKind = getTypeKind(element);
        if(elementTypeKind != TypeKind.DECLARED)
            throw new InvalidElementException("Element is not a declared type: " + elementTypeKind, element);

        if(!(element.asType() instanceof DeclaredType)) {
            throw new InvalidElementException("mirrorType expected to be DeclaredType but was " + element.asType(), element);
        }

        final DeclaredType declaredType = (DeclaredType) element.asType();
        return (TypeElement) declaredType.asElement();
    }

    public static List<Element> getElementsInTypeElement(TypeElement entity) {
        List<Element> elements = new ArrayList<Element>();
        Set<String> visitedTypes = new HashSet<String>();
        addElements(elements, entity, visitedTypes);
        return elements;
    }

    private static void addElements(List<Element> elements, TypeElement entity, Set<String> names) {
        for(Element element: entity.getEnclosedElements()) {
            String name = element.getSimpleName().toString();
            if(!names.contains(name)) {
                elements.add(element);
                names.add(name);
            }
        }

        TypeMirror parentMirror = entity.getSuperclass();
        if(parentMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) parentMirror;
            addElements(elements, (TypeElement) declaredType.asElement(), names);
        }
    }

    public static List<ExecutableElement> getMethodsInTypeElement(TypeElement typeElement) throws InvalidElementException {
        return mapItems(filter(getElementsInTypeElement(typeElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.METHOD && isAccessible(item);
            }
        }), new Function<ExecutableElement, Element>() {
            @Override
            public ExecutableElement apply(Element item) {
                return (ExecutableElement) item;
            }
        });
    }

    public static ExecutableElement findMethodWithName(String name, TypeElement typeElement) throws InvalidElementException {
        List<ExecutableElement> methods = getMethodsInTypeElement(typeElement);
        for(ExecutableElement method : methods) {
            if(name.equals(method.getSimpleName().toString())) {
                return method;
            }
        }
        return null;
    }

    public static boolean isString(Element element) {
        TypeKind typeKind = getTypeKind(element);
        if(typeKind == TypeKind.DECLARED) {
            final DeclaredType declaredType = (DeclaredType) element.asType();
            final TypeElement typeElement = (TypeElement) declaredType.asElement();
            final String typeName = typeElement.getQualifiedName().toString();
            return typeName.equals(ElementUtils.TYPE_STRING);
        }

        return false;
    }

    public static boolean isDate(Element element) {
        TypeKind typeKind = getTypeKind(element);
        if(typeKind == TypeKind.DECLARED) {
            final DeclaredType declaredType = (DeclaredType) element.asType();
            final TypeElement typeElement = (TypeElement) declaredType.asElement();
            final String typeName = typeElement.getQualifiedName().toString();
            return typeName.equals(ElementUtils.TYPE_DATE);
        }

        return false;
    }
}
