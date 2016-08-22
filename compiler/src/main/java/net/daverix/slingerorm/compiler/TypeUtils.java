package net.daverix.slingerorm.compiler;


import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class TypeUtils {
    public static boolean isTypeMirrorEqual(TypeMirror typeMirror, TypeMirror other) {
        if (typeMirror == other) return true;

        if (typeMirror.equals(other)) return true;

        if (typeMirror.getKind() == typeMirror.getKind() && typeMirror.getKind().isPrimitive())
            return true;

        if (typeMirror.getKind() == typeMirror.getKind() && typeMirror.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
            TypeElement otherTypeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();

            return typeElement.equals(otherTypeElement);
        }

        return false;
    }
}
