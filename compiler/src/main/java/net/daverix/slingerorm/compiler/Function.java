package net.daverix.slingerorm.compiler;

public interface Function<T, E> {
    T apply(E item);
}
