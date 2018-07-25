package net.daverix.slingerorm.compiler

import javax.lang.model.element.TypeElement


interface DatabaseEntityModelMap {
    operator fun get(element: TypeElement): DatabaseEntityModel
}
