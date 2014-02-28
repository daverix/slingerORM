package net.daverix.slingerorm.compiler;

public interface Predicate<T> {
    public boolean test(T item);
}