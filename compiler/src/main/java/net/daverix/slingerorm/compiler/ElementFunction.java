package net.daverix.slingerorm.compiler;


import javax.lang.model.element.Element;

interface ElementFunction<T extends Element, R> {
    R apply(T item) throws InvalidElementException;
}
