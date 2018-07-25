package net.daverix.slingerorm.compiler

import javax.lang.model.element.TypeElement

class DatabaseEntityModelLazyMap : DatabaseEntityModelMap {
    private val dbEntities = mutableMapOf<TypeElement,DatabaseEntityModel>()

    override fun get(element: TypeElement): DatabaseEntityModel {
        var entity = dbEntities[element]
        if (entity == null) {
            entity = DatabaseEntityModel(element)
            dbEntities[element] = entity
        }

        return entity
    }
}