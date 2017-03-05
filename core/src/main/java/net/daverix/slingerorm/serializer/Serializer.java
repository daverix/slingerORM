package net.daverix.slingerorm.serializer;

public interface Serializer<T,U> {
    U serialize(T value);

    T deserialize(U value);
}
