package net.daverix.slingerorm.compiler;

public class PackageProvider {
    public String getPackage(String qualifiedName) {
        if(qualifiedName == null) throw new IllegalArgumentException("qualifiedName is null");

        int lastDot = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, lastDot);
    }
}
