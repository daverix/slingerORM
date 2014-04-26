/*
 * Copyright 2014 David Laurell
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
package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.MappingFetcher;
import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.exception.FetchMappingException;
import net.daverix.slingerorm.serialization.DefaultSerializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LazyMappingFetcher implements MappingFetcher {
    private final Map<Class<?>, Mapping<?>> mMappings = new HashMap<Class<?>, Mapping<?>>();
    private final Map<Class<?>, Object> mSerializers = new HashMap<Class<?>, Object>();

    @Inject
    public LazyMappingFetcher() {
        mSerializers.put(DefaultSerializer.class, new DefaultSerializer());
    }

    @Override
    public <T> Mapping<T> fetchMapping(Class<T> entityClass) throws FetchMappingException {
        if(entityClass == null) throw new IllegalArgumentException("entityClass is null");

        Mapping<T> mapping = getExistingMapping(entityClass);
        if(mapping == null) {
            try {
                mapping = createMapping(entityClass);
            } catch (Exception e) {
                throw new FetchMappingException("Error getting mapping for entity " + entityClass, e);
            }

            if(mapping == null)
                throw new FetchMappingException("No mapping found for entity " + entityClass + ", has it been annotated with @DatabaseEntity?");

            putMapping(entityClass, mapping);
        }

        return mapping;
    }

    @Override
    public void registerTypeSerializer(Object typeSerializer) {
        mSerializers.put(typeSerializer.getClass(), typeSerializer);
    }

    protected <T> void putMapping(Class<T> entityClass, Mapping<T> mapping) {
        mMappings.put(entityClass, mapping);
    }

    @SuppressWarnings("unchecked")
    protected <T> Mapping<T> getExistingMapping(Class<T> entityClass) {
        return (Mapping<T>) mMappings.get(entityClass);
    }

    @SuppressWarnings("unchecked")
    protected <T> Mapping<T> createMapping(Class<T> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        Class<?> mappingClass = Class.forName(entityClass.getName() + "Mapping");

        DatabaseEntity entityAnnotation = entityClass.getAnnotation(DatabaseEntity.class);
        Class<?> serializerClass = entityAnnotation.serializer();

        Object serializer = mSerializers.get(serializerClass);
        if(serializer == null)
            throw new IllegalStateException("serializer " + serializerClass + " not registered. Please register it on the MappingFetcher class");

        Constructor<Mapping<T>> constructor = (Constructor<Mapping<T>>) mappingClass.getConstructor(serializerClass);
        if(constructor == null)
            throw new IllegalStateException("constructor was not found for " + mappingClass + " with argument " + serializerClass);

        return constructor.newInstance(serializer);
    }
}
