package net.daverix.slingerorm.compiler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public interface TypeElementConverter {
    public TypeElement asTypeElement(TypeMirror typeMirror);
}
