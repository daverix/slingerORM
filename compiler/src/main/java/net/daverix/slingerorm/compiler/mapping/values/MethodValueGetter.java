package net.daverix.slingerorm.compiler.mapping.values;


import net.daverix.slingerorm.compiler.mapping.Getter;

public class MethodValueGetter implements Getter {
    private final String methodName;

    public MethodValueGetter(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String get(String itemVariable) {
        return String.format("%s.%s()", itemVariable, methodName);
    }
}
