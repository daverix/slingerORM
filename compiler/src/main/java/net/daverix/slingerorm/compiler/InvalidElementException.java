package net.daverix.slingerorm.compiler;

import javax.lang.model.element.Element;

class InvalidElementException extends Exception {
    private final Element element;

    public InvalidElementException(String s, Element element) {
        super(s);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
