package net.daverix.slingerorm.serialization;

public interface TypeSerializer<TClassValue,TDatabaseValue> {
    TDatabaseValue serialize(TClassValue in);
    TClassValue deserialize(TDatabaseValue in);
}
