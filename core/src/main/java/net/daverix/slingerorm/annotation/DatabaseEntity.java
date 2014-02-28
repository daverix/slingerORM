package net.daverix.slingerorm.annotation;

import net.daverix.slingerorm.serialization.DefaultSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as an entity that can be mapped to a database table. The optional value is used for
 * the table name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DatabaseEntity {
    public String name() default "";

    /**
     * Use this property if you can't annotate a field in a superclass
     * @return the primary key
     */
    public String primaryKey() default "";

    public Class<?> serializer() default DefaultSerializer.class;
}
