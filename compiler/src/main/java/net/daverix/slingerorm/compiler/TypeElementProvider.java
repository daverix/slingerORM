package net.daverix.slingerorm.compiler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class TypeElementProvider {
    private final Elements elements;

    public TypeElementProvider(Elements elements) {
        this.elements = elements;
    }

    public TypeElement getTypeElement(String qualifiedName) {
        return elements.getTypeElement(qualifiedName);
    }
}
