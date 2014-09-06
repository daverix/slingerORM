package net.daverix.slingerorm;

public interface StorageFactory {
    <T> Storage<T> build(Class<T> entityClass);
}
