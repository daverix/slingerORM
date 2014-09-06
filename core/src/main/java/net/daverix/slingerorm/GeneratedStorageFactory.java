package net.daverix.slingerorm;

import javax.inject.Inject;

public class GeneratedStorageFactory implements StorageFactory {
    @Inject
    public GeneratedStorageFactory() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Storage<T> build(Class<T> entityClass) {
        if(entityClass == null) throw new NullPointerException("entityClass");
        try {
            return (Storage<T>) Class.forName(entityClass.getName() + "Storage").newInstance();
        }
        catch (Exception e) {
            throw new IllegalStateException(String.format("could not getting storage for %s",
                    entityClass.getName()), e);
        }
    }
}
