package net.daverix.slingerorm;

public interface Serializer<T,U> {
    U serialize(T value);

    T deserialize(U value);
}
