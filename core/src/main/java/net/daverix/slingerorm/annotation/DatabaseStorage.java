package net.daverix.slingerorm.annotation;

import net.daverix.slingerorm.serialization.DefaultSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DatabaseStorage {
    Class<?> serializer() default DefaultSerializer.class;
}
