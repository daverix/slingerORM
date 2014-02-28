package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.annotation.FieldName;
import net.daverix.slingerorm.annotation.NotDatabaseField;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.util.Set;

import static net.daverix.slingerorm.compiler.ElementUtils.TYPE_STRING;
import static net.daverix.slingerorm.compiler.ElementUtils.getTypeKind;

public class EntityField {
    private final Element mField;

    public EntityField(Element field) {
        if(field == null) throw new IllegalArgumentException("field is null");
        mField = field;
    }


}
