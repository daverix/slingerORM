package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.exception.FetchMappingException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by daverix on 2/1/14.
 */
public class LazyMappingFetcher implements MappingFetcher {
    private final Map<Class<?>, Mapping<?>> mMappings = new HashMap<Class<?>, Mapping<?>>();

    public LazyMappingFetcher() {
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

    protected <T> void putMapping(Class<T> entityClass, Mapping<T> mapping) {
        mMappings.put(entityClass, mapping);
    }

    @SuppressWarnings("unchecked")
    protected <T> Mapping<T> getExistingMapping(Class<T> entityClass) {
        return (Mapping<T>) mMappings.get(entityClass);
    }

    @SuppressWarnings("unchecked")
    protected <T> Mapping<T> createMapping(Class<T> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Class<?> mappingClass = Class.forName(entityClass.getName() + "Mapping");
        return (Mapping<T>) mappingClass.newInstance();
    }
}
