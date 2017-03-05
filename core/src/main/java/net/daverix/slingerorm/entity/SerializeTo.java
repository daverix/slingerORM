package net.daverix.slingerorm.entity;


import net.daverix.slingerorm.serializer.SerializeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface SerializeTo {
    SerializeType value();
}
