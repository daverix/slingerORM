package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class InsertMethod implements StorageMethod {
    private final String methodName;
    private String databaseEntityTypeName;
    private final String databaseEntityTypeQualifiedName;
    private final MapperDescription mapperDescription;

    public InsertMethod(String methodName,
                        String databaseEntityTypeName,
                        String databaseEntityTypeQualifiedName,
                        MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.databaseEntityTypeName = databaseEntityTypeName;
        this.databaseEntityTypeQualifiedName = databaseEntityTypeQualifiedName;
        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db, " + databaseEntityTypeName + " item) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"item is null\");\n");
        writer.write("\n");
        writer.write("        ContentValues values = new ContentValues();\n");
        writer.write("        " + mapperDescription.getVariableName() + ".mapValues(item, values);\n");
        writer.write("        db.insertOrThrow(" + mapperDescription.getVariableName() + ".getTableName(), null, values);\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList("android.database.sqlite.SQLiteDatabase",
                "android.content.ContentValues",
                databaseEntityTypeQualifiedName);
    }

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
