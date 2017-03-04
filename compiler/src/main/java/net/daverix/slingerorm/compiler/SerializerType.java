package net.daverix.slingerorm.compiler;


import java.util.List;

class SerializerType {
    private final String name;
    private final String type;
    private final List<String> imports;

    SerializerType(String name, String type, List<String> imports) {
        this.name = name;
        this.type = type;
        this.imports = imports;
    }

    String getName() {
        return name;
    }

    String getType() {
        return type;
    }

    public List<String> getImports() {
        return imports;
    }
}
