package net.daverix.slingerorm.compiler

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic.Kind.ERROR

fun Messager.printError(message: String) = printMessage(ERROR, message)

fun Messager.printError(message: String, element: Element) = printMessage(ERROR, message, element)
