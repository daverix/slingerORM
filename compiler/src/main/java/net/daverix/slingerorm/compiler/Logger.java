package net.daverix.slingerorm.compiler;

import javax.lang.model.element.Element;

public interface Logger {
    void debug(String message, Element element);
    void debug(String message);
}
