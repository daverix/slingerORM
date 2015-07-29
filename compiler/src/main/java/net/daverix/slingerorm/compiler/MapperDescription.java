package net.daverix.slingerorm.compiler;

class MapperDescription {
    private String qualifiedName;
    private String simpleName;
    private boolean emptyConstructor;

    MapperDescription(String qualifiedName, String simpleName, boolean emptyConstructor) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.emptyConstructor = emptyConstructor;
    }

    public String getQualifiedName() {
        return qualifiedName + "Mapper";
    }

    public String getEntityName() {
        return simpleName;
    }

    public boolean hasEmptyConstructor() {
        return emptyConstructor;
    }

    public String getVariableName() {
        String firstCharacterLowerCaseName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
        return firstCharacterLowerCaseName + "Mapper";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapperDescription)) return false;

        MapperDescription that = (MapperDescription) o;

        return qualifiedName.equals(that.qualifiedName);

    }

    @Override
    public int hashCode() {
        return qualifiedName.hashCode();
    }
}
