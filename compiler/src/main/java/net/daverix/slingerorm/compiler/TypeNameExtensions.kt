package net.daverix.slingerorm.compiler

import com.squareup.javapoet.TypeName
import java.lang.reflect.Type
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror


inline val TypeMirror.typeName: TypeName get() = TypeName.get(this)

inline val Element.typeName: TypeName get() = asType().typeName

inline val Type.typeName: TypeName get() = TypeName.get(this)