package net.daverix.snakedb.mapping;

import net.daverix.snakedb.exception.FetchMappingException;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by daverix on 2/1/14.
 */
public class MappingFetcher implements IMappingFetcher {
    private final Map<Class<?>, IMapping<?>> mMappings = new HashMap<Class<?>, IMapping<?>>();
    private final Set<Class<?>> mEntityClasses = new LinkedHashSet<Class<?>>();

    public MappingFetcher() {
    }

    @Override
    public void registerEntity(Class<?> entityClass) {
        mEntityClasses.add(entityClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IMapping<T> getMapping(Class<T> entityClass) throws FetchMappingException {
        IMapping<T> mapping = (IMapping<T>) mMappings.get(entityClass);
        if(mapping == null)
            throw new FetchMappingException("No mapping found for entityClass " + entityClass);

        return mapping;
    }

    @Override
    public void initialize() throws FetchMappingException {
        try {
            for(Class<?> entityClass : mEntityClasses) {
                mMappings.put(entityClass, createMapping(entityClass));
            }
        } catch (Exception e) {
            throw new FetchMappingException("Could not initialize registered mappings", e);
        }
    }

    public IMapping<?> createMapping(Class<?> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Class<?> mappingClass = Class.forName(entityClass.getName() + "Mapping");
        return (IMapping<?>) mappingClass.newInstance();
    }
}
