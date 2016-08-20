package net.daverix.slingerorm.android.entities;

import net.daverix.slingerorm.serialization.TypeSerializer;

public class MyObjectSerializer implements TypeSerializer<MyObject, String> {
    @Override
    public String serialize(MyObject in) {
        return in.getMyValue();
    }

    @Override
    public MyObject deserialize(String in) {
        return new MyObject(in);
    }
}
