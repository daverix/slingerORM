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

/**
 * Created by daverix on 2/1/14.
 */
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
