package net.daverix.slingerorm.compiler;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.daverix.slingerorm.compiler.ListUtils.filter;
import static net.daverix.slingerorm.compiler.ListUtils.mapItems;

public final class ElementUtils {
    public static final String TYPE_STRING = "java.lang.String";

    private ElementUtils(){}

    public static TypeKind getTypeKind(Element element) {
        final TypeMirror fieldType = element.asType();
        return fieldType.getKind();
    }

    public static boolean isAccessable(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("PRIVATE".equals(name) ||
                    "PROTECTED".equals(name) ||
                    "STATIC".equals(name) ||
                    "TRANSIENT".equals(name)) {
                return false;
            }
        }

        return true;
    }

    public static String getDeclaredTypeName(Element element) {
        if(element == null) throw new IllegalArgumentException("element is null");
        return getTypeElement(element).getQualifiedName().toString();
    }

    public static TypeElement getTypeElement(Element element) {
        if(element == null) throw new IllegalArgumentException("element is null");

        TypeKind elementTypeKind = getTypeKind(element);
        if(elementTypeKind != TypeKind.DECLARED)
            throw new IllegalArgumentException("Element is not a declared type: " + elementTypeKind);

        if(!(element.asType() instanceof DeclaredType)) {
            throw new IllegalStateException("mirrorType expected to be DeclaredType but was " + element.asType());
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

    public static List<ExecutableElement> getMethodsInTypeElement(TypeElement typeElement) {
        return mapItems(filter(getElementsInTypeElement(typeElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.METHOD && isAccessable(item);
            }
        }), new Function<ExecutableElement, Element>() {
            @Override
            public ExecutableElement apply(Element item) {
                return (ExecutableElement) item;
            }
        });
    }
}
