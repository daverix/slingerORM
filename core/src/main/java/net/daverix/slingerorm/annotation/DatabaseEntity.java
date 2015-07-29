/*
 * Copyright 2015 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    String name() default "";

    /**
     * Use this property if you can't annotate a field in a superclass
     * @return the primary key
     */
    String primaryKeyField() default "";

    /**
     * If you have other than native types, you need to set a custom serializer
     * @return a class with methods annotated with either {@link DeserializeType} or {@link SerializeType}.
     */
    Class<?> serializer() default DefaultSerializer.class;
}
