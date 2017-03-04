package net.daverix.slingerorm;


public class BooleanSerializer implements Serializer<Boolean,Integer> {
    @Override
    public Integer serialize(Boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public Boolean deserialize(Integer value) {
        return value == 1;
    }
}
